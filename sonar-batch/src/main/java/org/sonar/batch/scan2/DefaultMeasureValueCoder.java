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
package org.sonar.batch.scan2;

import com.persistit.Value;
import com.persistit.encoding.CoderContext;
import com.persistit.encoding.ValueCoder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasureBuilder;

import java.io.Serializable;

class DefaultMeasureValueCoder implements ValueCoder {

  private MetricFinder metricFinder;

  public DefaultMeasureValueCoder(MetricFinder metricFinder) {
    this.metricFinder = metricFinder;
  }

  @Override
  public void put(Value value, Object object, CoderContext context) {
    DefaultMeasure m = (DefaultMeasure) object;
    value.put(m.inputFile());
    value.putUTF(m.metric().key());
    value.put(m.value());
  }

  @Override
  public Object get(Value value, Class clazz, CoderContext context) {
    DefaultMeasureBuilder builder = new DefaultMeasureBuilder();
    InputFile f = (InputFile) value.get();
    if (f != null) {
      builder.onFile(f);
    } else {
      builder.onProject();
    }
    Metric m = metricFinder.findByKey(value.getString());
    builder.forMetric(m);
    builder.withValue((Serializable) value.get());
    return builder.build();
  }

}
