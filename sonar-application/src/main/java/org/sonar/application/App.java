/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.application;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.JmxUtils;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitor;
import org.sonar.process.ProcessLogging;
import org.sonar.process.ProcessMXBean;
import org.sonar.process.ProcessUtils;
import org.sonar.process.ProcessWrapper;
import org.sonar.process.Props;

import java.io.File;
import java.util.Properties;

/**
 * Entry-point of process that starts and monitors elasticsearch and web servers
 */
public class App implements ProcessMXBean {

  private Monitor monitor = new Monitor();
  private ProcessWrapper elasticsearch;
  private ProcessWrapper server;
  private boolean success = false;

  public App() throws Exception {
    JmxUtils.registerMBean(this, "SonarQube");
    ProcessUtils.addSelfShutdownHook(this);
  }

  public void start(Props props) throws InterruptedException {
    try {
      Logger logger = LoggerFactory.getLogger(getClass());
      monitor.start();

      File homeDir = props.fileOf("sonar.path.home");
      File tempDir = props.fileOf("sonar.path.temp");
      elasticsearch = new ProcessWrapper(JmxUtils.SEARCH_SERVER_NAME)
        .setWorkDir(homeDir)
        .setJmxPort(props.intOf(DefaultSettings.SEARCH_JMX_PORT))
        .addJavaOpts(props.of(DefaultSettings.SEARCH_JAVA_OPTS))
        .setTempDirectory(tempDir.getAbsoluteFile())
        .setClassName("org.sonar.search.SearchServer")
        .addProperties(props.rawProperties())
        .addClasspath("./lib/common/*")
        .addClasspath("./lib/search/*");
      if (elasticsearch.execute()) {
        monitor.registerProcess(elasticsearch);
        if (elasticsearch.waitForReady()) {
          logger.info("search server is up");

          // do not yet start SQ in cluster mode. See SONAR-5483 & SONAR-5391
          if (StringUtils.isEmpty(props.of(DefaultSettings.CLUSTER_MASTER, null))) {
            server = new ProcessWrapper(JmxUtils.WEB_SERVER_NAME)
              .setWorkDir(homeDir)
              .setJmxPort(props.intOf(DefaultSettings.WEB_JMX_PORT))
              .addJavaOpts(props.of(DefaultSettings.WEB_JAVA_OPTS))
              .setTempDirectory(tempDir.getAbsoluteFile())
              // required for logback tomcat valve
              .setLogDir(props.fileOf("sonar.path.logs"))
              .setClassName("org.sonar.server.app.WebServer")
              .addProperties(props.rawProperties())
              .addClasspath("./lib/common/*")
              .addClasspath("./lib/server/*");
            String driverPath = props.of(JdbcSettings.PROPERTY_DRIVER_PATH);
            if (driverPath != null) {
              server.addClasspath(driverPath);
            }
            if (server.execute()) {
              monitor.registerProcess(server);
              if (server.waitForReady()) {
                success = true;
                logger.info("web server is up");
                monitor.join();
              }
            }
          } else {
            success = true;
            monitor.join();
          }
        }
      }
    } finally {
      terminate();
    }
  }

  static String starPath(File homeDir, String relativePath) {
    File dir = new File(homeDir, relativePath);
    return FilenameUtils.concat(dir.getAbsolutePath(), "*");
  }

  @Override
  public boolean isReady() {
    return monitor.isAlive();
  }

  @Override
  public long ping() {
    return System.currentTimeMillis();
  }

  @Override
  public void terminate() {
    if (monitor != null && monitor.isAlive()) {
      monitor.terminate();
      monitor.interrupt();
      monitor = null;
    }
    if (server != null) {
      server.terminate();
      server = null;
    }
    if (elasticsearch != null) {
      elasticsearch.terminate();
      elasticsearch = null;
    }
  }

  private boolean isSuccess() {
    return success;
  }

  public static void main(String[] args) throws Exception {
    new MinimumViableSystem().check();
    CommandLineParser cli = new CommandLineParser();
    Properties rawProperties = cli.parseArguments(args);
    Props props = new PropsBuilder(rawProperties, new JdbcSettings()).build();
    new ProcessLogging().configure(props, "/org/sonar/application/logback.xml");
    App app = new App();

    // start and wait for shutdown command
    app.start(props);

    LoggerFactory.getLogger(App.class).info("stopped");
    System.exit(app.isSuccess() ? 0 : 1);
  }
}
