package org.apache.mahout.text;
/*
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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.lucene.index.SegmentInfoPerCommit;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.store.IOContext;

import java.io.IOException;

/**
 * {@link RecordReader} implementation for Lucene segments. Each {@link InputSplit} contains a separate Lucene segment.
 * Emits records consisting of a {@link Text} document ID and a null key.
 */
public class LuceneSegmentRecordReader extends RecordReader<Text, NullWritable> {

  public static final int USE_TERM_INFO = 1;

  private SegmentReader segmentReader;
  private Scorer scorer;

  private int nextDocId;
  private Text key = new Text();

  @Override
  public void initialize(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
    LuceneSegmentInputSplit inputSplit = (LuceneSegmentInputSplit) split;

    Configuration configuration = context.getConfiguration();
    LuceneStorageConfiguration lucene2SeqConfiguration = new LuceneStorageConfiguration(configuration);

    SegmentInfoPerCommit segmentInfo = inputSplit.getSegment(configuration);
    segmentReader = new SegmentReader(segmentInfo, USE_TERM_INFO, IOContext.READ);


    IndexSearcher searcher = new IndexSearcher(segmentReader);
    Weight weight = lucene2SeqConfiguration.getQuery().createWeight(searcher);
    scorer = weight.scorer(segmentReader.getContext(), false, false, null);
    if (scorer == null) {
      throw new IllegalArgumentException("Could not create query scorer for query: " + lucene2SeqConfiguration.getQuery());
    }
  }

  @Override
  public boolean nextKeyValue() throws IOException, InterruptedException {
    nextDocId = scorer.nextDoc();

    return nextDocId != Scorer.NO_MORE_DOCS;
  }

  @Override
  public Text getCurrentKey() throws IOException, InterruptedException {
    key.set(String.valueOf(nextDocId));
    return key;
  }

  @Override
  public NullWritable getCurrentValue() throws IOException, InterruptedException {
    return NullWritable.get();
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {
    return scorer.cost() == 0 ? 0 : (float) nextDocId / scorer.cost();//this is a rough estimate, due to the possible inaccuracies of cost
  }

  @Override
  public void close() throws IOException {
    segmentReader.close();
    //searcher.close();
  }
}
