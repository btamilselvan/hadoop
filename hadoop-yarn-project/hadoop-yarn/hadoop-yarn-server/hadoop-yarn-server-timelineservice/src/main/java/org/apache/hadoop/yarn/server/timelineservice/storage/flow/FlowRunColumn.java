/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.timelineservice.storage.flow;

import java.io.IOException;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.yarn.server.timelineservice.storage.common.Column;
import org.apache.hadoop.yarn.server.timelineservice.storage.common.ColumnFamily;
import org.apache.hadoop.yarn.server.timelineservice.storage.common.ColumnHelper;
import org.apache.hadoop.yarn.server.timelineservice.storage.common.Separator;
import org.apache.hadoop.yarn.server.timelineservice.storage.common.TimelineStorageUtils;
import org.apache.hadoop.yarn.server.timelineservice.storage.common.TypedBufferedMutator;

/**
 * Identifies fully qualified columns for the {@link FlowRunTable}.
 */
public enum FlowRunColumn implements Column<FlowRunTable> {

  /**
   * When the flow was started. This is the minimum of currently known
   * application start times.
   */
  MIN_START_TIME(FlowRunColumnFamily.INFO, "min_start_time",
      AggregationOperation.MIN),

  /**
   * When the flow ended. This is the maximum of currently known application end
   * times.
   */
  MAX_END_TIME(FlowRunColumnFamily.INFO, "max_end_time",
      AggregationOperation.MAX),

  /**
   * The version of the flow that this flow belongs to.
   */
  FLOW_VERSION(FlowRunColumnFamily.INFO, "flow_version", null);

  private final ColumnHelper<FlowRunTable> column;
  private final ColumnFamily<FlowRunTable> columnFamily;
  private final String columnQualifier;
  private final byte[] columnQualifierBytes;
  private final AggregationOperation aggOp;

  private FlowRunColumn(ColumnFamily<FlowRunTable> columnFamily,
      String columnQualifier, AggregationOperation aggOp) {
    this.columnFamily = columnFamily;
    this.columnQualifier = columnQualifier;
    this.aggOp = aggOp;
    // Future-proof by ensuring the right column prefix hygiene.
    this.columnQualifierBytes = Bytes.toBytes(Separator.SPACE
        .encode(columnQualifier));
    this.column = new ColumnHelper<FlowRunTable>(columnFamily);
  }

  /**
   * @return the column name value
   */
  private String getColumnQualifier() {
    return columnQualifier;
  }

  public byte[] getColumnQualifierBytes() {
    return columnQualifierBytes.clone();
  }

  public AggregationOperation getAggregationOperation() {
    return aggOp;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.apache.hadoop.yarn.server.timelineservice.storage.common.Column#store
   * (byte[], org.apache.hadoop.yarn.server.timelineservice.storage.common.
   * TypedBufferedMutator, java.lang.Long, java.lang.Object,
   * org.apache.hadoop.yarn.server.timelineservice.storage.flow.Attribute[])
   */
  public void store(byte[] rowKey,
      TypedBufferedMutator<FlowRunTable> tableMutator, Long timestamp,
      Object inputValue, Attribute... attributes) throws IOException {

    Attribute[] combinedAttributes = TimelineStorageUtils.combineAttributes(
        attributes, aggOp);
    column.store(rowKey, tableMutator, columnQualifierBytes, timestamp,
        inputValue, combinedAttributes);
  }

  public Object readResult(Result result) throws IOException {
    return column.readResult(result, columnQualifierBytes);
  }

  /**
   * Retrieve an {@link FlowRunColumn} given a name, or null if there is no
   * match. The following holds true: {@code columnFor(x) == columnFor(y)} if
   * and only if {@code x.equals(y)} or {@code (x == y == null)}
   *
   * @param columnQualifier
   *          Name of the column to retrieve
   * @return the corresponding {@link FlowRunColumn} or null
   */
  public static final FlowRunColumn columnFor(String columnQualifier) {

    // Match column based on value, assume column family matches.
    for (FlowRunColumn ec : FlowRunColumn.values()) {
      // Find a match based only on name.
      if (ec.getColumnQualifier().equals(columnQualifier)) {
        return ec;
      }
    }

    // Default to null
    return null;
  }

  /**
   * Retrieve an {@link FlowRunColumn} given a name, or null if there is no
   * match. The following holds true: {@code columnFor(a,x) == columnFor(b,y)}
   * if and only if {@code a.equals(b) & x.equals(y)} or
   * {@code (x == y == null)}
   *
   * @param columnFamily
   *          The columnFamily for which to retrieve the column.
   * @param name
   *          Name of the column to retrieve
   * @return the corresponding {@link FlowRunColumn} or null if both arguments
   *         don't match.
   */
  public static final FlowRunColumn columnFor(FlowRunColumnFamily columnFamily,
      String name) {

    for (FlowRunColumn ec : FlowRunColumn.values()) {
      // Find a match based column family and on name.
      if (ec.columnFamily.equals(columnFamily)
          && ec.getColumnQualifier().equals(name)) {
        return ec;
      }
    }

    // Default to null
    return null;
  }

}
