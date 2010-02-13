/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.cf.taste.hadoop.item;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

public final class UserVectorToCooccurrenceMapper extends MapReduceBase implements
    Mapper<LongWritable,VectorWritable,IntWritable,IntWritable> {
  
  @Override
  public void map(LongWritable userID,
                  VectorWritable userVector,
                  OutputCollector<IntWritable,IntWritable> output,
                  Reporter reporter) throws IOException {
    Iterator<Vector.Element> it = userVector.get().iterateNonZero();
    while (it.hasNext()) {
      Vector.Element next1 = it.next();
      int index1 = next1.index();
      Iterator<Vector.Element> it2 = userVector.get().iterateNonZero();
      IntWritable itemWritable1 = new IntWritable(index1);
      while (it2.hasNext()) {
        Vector.Element next2 = it2.next();
        int index2 = next2.index();
        if (index1 != index2) {
          output.collect(itemWritable1, new IntWritable(index2));
        }
      }
    }
  }
  
}
