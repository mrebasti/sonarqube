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
package org.sonar.server.db.migrations;

import org.junit.Test;
import org.slf4j.Logger;
import org.sonar.server.db.migrations.SqlUtil;

import java.sql.SQLException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SqlUtilTest {

  @Test
  public void log_all_sql_exceptions() {
    SQLException root = new SQLException("this is root", "123");
    SQLException next = new SQLException("this is next", "456");
    root.setNextException(next);

    Logger logger = mock(Logger.class);
    SqlUtil.log(logger, root);

    verify(logger).error("SQL error: {}. Message: {}", "456", "this is next");
  }
}
