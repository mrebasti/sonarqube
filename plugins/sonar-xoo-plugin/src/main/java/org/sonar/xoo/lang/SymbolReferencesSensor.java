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
package org.sonar.xoo.lang;

import com.google.common.base.Splitter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.symbol.Symbol;
import org.sonar.api.batch.sensor.symbol.SymbolTableBuilder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.xoo.Xoo;
import org.sonar.xoo.XooConstants;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Parse files *.xoo.symbol
 */
public class SymbolReferencesSensor implements Sensor {

  private static final String SYMBOL_EXTENSION = ".symbol";

  private void processFileHighlighting(InputFile inputFile, SensorContext context) {
    File ioFile = inputFile.file();
    File symbolFile = new File(ioFile.getParentFile(), ioFile.getName() + SYMBOL_EXTENSION);
    if (symbolFile.exists()) {
      XooConstants.LOG.debug("Processing " + symbolFile.getAbsolutePath());
      try {
        List<String> lines = FileUtils.readLines(symbolFile, context.fileSystem().encoding().name());
        int lineNumber = 0;
        SymbolTableBuilder symbolTableBuilder = context.symbolTableBuilder(inputFile);
        for (String line : lines) {
          lineNumber++;
          if (StringUtils.isBlank(line)) {
            continue;
          }
          if (line.startsWith("#")) {
            continue;
          }
          try {
            Iterator<String> split = Splitter.on(",").split(line).iterator();
            int startOffset = Integer.parseInt(split.next());
            int endOffset = Integer.parseInt(split.next());
            Symbol s = symbolTableBuilder.newSymbol(startOffset, endOffset);
            while (split.hasNext()) {
              symbolTableBuilder.newReference(s, Integer.parseInt(split.next()));
            }
          } catch (Exception e) {
            throw new IllegalStateException("Error processing line " + lineNumber + " of file " + symbolFile.getAbsolutePath(), e);
          }
        }
        symbolTableBuilder.done();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Xoo Symbol Reference Sensor")
      .provides(CoreMetrics.LINES)
      .workOnLanguages(Xoo.KEY)
      .workOnFileTypes(InputFile.Type.MAIN, InputFile.Type.TEST);
  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile file : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguages(Xoo.KEY))) {
      processFileHighlighting(file, context);
    }
  }
}
