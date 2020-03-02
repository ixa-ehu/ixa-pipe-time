/*
 *  Copyright 2018 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eus.ixa.ixa.pipe.time;

import eus.ixa.ixa.pipe.ml.StatisticalSequenceLabeler;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabel;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.utils.Span;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Timex3;
import ixa.kaflib.WF;

import java.util.List;
import java.util.Properties;

/**
 * Annotation class for temporal expressions in ixa-pipe-time. Use this class for
 * examples on using ixa-pipe-ml API for Sequence Labelling.
 * 
 * @author ragerri
 * @version 2018-05-14
 * 
 */
public class Annotate {

  /**
   * The SequenceLabeler to do the annotation.
   */
  private StatisticalSequenceLabeler temporalTagger;
  /**
   * Clear features after every sentence or when a -DOCSTART- mark appears.
   */
  private String clearFeatures;

  public Annotate(final Properties properties) {

    this.clearFeatures = properties.getProperty("clearFeatures");
    temporalTagger = new StatisticalSequenceLabeler(properties);
  }

  public final void annotateTimeToKAF(final KAFDocument kaf) {

    List<List<WF>> sentences = kaf.getSentences();

    for (List<WF> sentence : sentences) {
      // process each sentence
      String[] tokens = new String[sentence.size()];
      for (int i = 0; i < sentence.size(); i++) {
        tokens[i] = sentence.get(i).getForm();
      }
      if (clearFeatures.equalsIgnoreCase("docstart")
          && tokens[0].startsWith("-DOCSTART-")) {
        temporalTagger.clearAdaptiveData();
      }
      Span[] statSpans = temporalTagger.seqToSpans(tokens);
      Span[] allSpansArray = SequenceLabelerME.dropOverlappingSpans(statSpans);
      List<SequenceLabel> names = temporalTagger.getSequencesFromSpans(tokens,
          allSpansArray);
      for (SequenceLabel name : names) {
        int startIndex = name.getSpan().getStart();
        int endIndex = name.getSpan().getEnd();
        List<WF> nameWFs = sentence.subList(startIndex, endIndex);
        ixa.kaflib.Span<WF> neSpan = KAFDocument.newWFSpan(nameWFs);
        Timex3 timex3 = kaf.newTimex3(name.getType());
        timex3.setSpan(neSpan);
      }
      if (clearFeatures.equalsIgnoreCase("yes")) {
        temporalTagger.clearAdaptiveData();
      }
    }
    if (clearFeatures.equalsIgnoreCase("yes")) {
      temporalTagger.clearAdaptiveData();
    }
  }

  public String annotateToTimeML(KAFDocument kaf) {

    return kaf.toString();
  }

}
