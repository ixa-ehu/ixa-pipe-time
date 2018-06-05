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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Lists;

import eus.ixa.ixa.pipe.ml.StatisticalSequenceLabeler;
import eus.ixa.ixa.pipe.ml.nerc.DictionariesNERTagger;
import eus.ixa.ixa.pipe.ml.nerc.NumericNERTagger;
import eus.ixa.ixa.pipe.ml.resources.Dictionaries;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabel;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelSample;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelerME;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import eus.ixa.ixa.pipe.ml.utils.Span;
import eus.ixa.ixa.pipe.ml.utils.StringUtils;
import ixa.kaflib.Entity;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

/**
 * Annotation class for Named Entities in ixa-pipe-time. Use this class for
 * examples on using ixa-pipe-ml API for Named Entity tagging.
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

  public Annotate(final Properties properties) throws IOException {

    this.clearFeatures = properties.getProperty("clearFeatures");
    temporalTagger = new StatisticalSequenceLabeler(properties);
  }


  public final void annotateTimeToKAF(final KAFDocument kaf) throws IOException {

    List<List<WF>> sentences = kaf.getSentences();

    for (List<WF> sentence : sentences) {
      // process each sentence
      String[] tokens = new String[sentence.size()];
      String[] tokenIds = new String[sentence.size()];
      for (int i = 0; i < sentence.size(); i++) {
        tokens[i] = sentence.get(i).getForm();
        tokenIds[i] = sentence.get(i).getId();
      }
      if (clearFeatures.equalsIgnoreCase("docstart")
            && tokens[0].startsWith("-DOCSTART-")) {
          temporalTagger.clearAdaptiveData();
      }
      Span[] statSpans = temporalTagger.seqToSpans(tokens);
      Span[] allSpansArray = SequenceLabelerME
          .dropOverlappingSpans(statSpans);
      List<SequenceLabel> names = temporalTagger.getSequencesFromSpans(tokens, allSpansArray);
      for (SequenceLabel name : names) {
        Integer startIndex = name.getSpan().getStart();
        Integer endIndex = name.getSpan().getEnd();
        List<String> wfIds = Arrays
            .asList(Arrays.copyOfRange(tokenIds, startIndex, endIndex));
        List<String> wfTermIds = getAllWFIdsFromTerms(kaf);
        if (checkTermsRefsIntegrity(wfIds, wfTermIds)) {
          List<Term> nameTerms = kaf.getTermsFromWFs(wfIds);
          ixa.kaflib.Span<Term> neSpan = KAFDocument.newTermSpan(nameTerms);
          List<ixa.kaflib.Span<Term>> references = new ArrayList<ixa.kaflib.Span<Term>>();
          references.add(neSpan);
          Entity neEntity = kaf.newEntity(references);
          neEntity.setType(name.getType());
        }
      }
      if (clearFeatures.equalsIgnoreCase("yes")) {
        temporalTagger.clearAdaptiveData();
      }
    }
    if (clearFeatures.equalsIgnoreCase("yes")) {
      temporalTagger.clearAdaptiveData();
    }
  }

  /**
   * Get all the WF ids for the terms contained in the KAFDocument.
   * 
   * @param kaf
   *          the KAFDocument
   * @return the list of all WF ids in the terms layer
   */
  public List<String> getAllWFIdsFromTerms(KAFDocument kaf) {
    List<Term> terms = kaf.getTerms();
    List<String> wfTermIds = new ArrayList<>();
    for (int i = 0; i < terms.size(); i++) {
      List<WF> sentTerms = terms.get(i).getWFs();
      for (WF form : sentTerms) {
        wfTermIds.add(form.getId());
      }
    }
    return wfTermIds;
  }

  /**
   * Check that the references from the entity spans are actually contained in
   * the term ids.
   * 
   * @param wfIds
   *          the worform ids corresponding to the Term span
   * @param termWfIds
   *          all the terms in the document
   * @return true or false
   */
  public boolean checkTermsRefsIntegrity(List<String> wfIds,
      List<String> termWfIds) {
    for (int i = 0; i < wfIds.size(); i++) {
      if (!termWfIds.contains(wfIds.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Enumeration class for CoNLL 2003 BIO format
   */
  private static enum BIO {
    BEGIN("B-"), IN("I-"), OUT("O");
    String tag;

    BIO(String tag) {
      this.tag = tag;
    }

    public String toString() {
      return this.tag;
    }
  }

  /**
   * Output Conll2002 format.
   * 
   * @param kaf
   *          the kaf document
   * @return the annotated named entities in conll02 format
   */
  public String annotateNEsToCoNLL2002(KAFDocument kaf) {
    List<Entity> namedEntityList = kaf.getEntities();
    Map<String, Integer> entityToSpanSize = new HashMap<String, Integer>();
    Map<String, String> entityToType = new HashMap<String, String>();
    for (Entity ne : namedEntityList) {
      List<ixa.kaflib.Span<Term>> entitySpanList = ne.getSpans();
      for (ixa.kaflib.Span<Term> spanTerm : entitySpanList) {
        Term neTerm = spanTerm.getFirstTarget();
        entityToSpanSize.put(neTerm.getId(), spanTerm.size());
        entityToType.put(neTerm.getId(), ne.getType());
      }
    }

    List<List<WF>> sentences = kaf.getSentences();
    StringBuilder sb = new StringBuilder();
    for (List<WF> sentence : sentences) {
      int sentNumber = sentence.get(0).getSent();
      List<Term> sentenceTerms = kaf.getSentenceTerms(sentNumber);

      for (int i = 0; i < sentenceTerms.size(); i++) {
        Term thisTerm = sentenceTerms.get(i);

        if (entityToSpanSize.get(thisTerm.getId()) != null) {
          int neSpanSize = entityToSpanSize.get(thisTerm.getId());
          String neClass = entityToType.get(thisTerm.getId());
          String neType = convertToConLLTypes(neClass);
          if (neSpanSize > 1) {
            for (int j = 0; j < neSpanSize; j++) {
              thisTerm = sentenceTerms.get(i + j);
              sb.append(thisTerm.getForm());
              sb.append("\t");
              sb.append(thisTerm.getLemma());
              sb.append("\t");
              sb.append(thisTerm.getMorphofeat());
              sb.append("\t");
              if (j == 0) {
                sb.append(BIO.BEGIN.toString());
              } else {
                sb.append(BIO.IN.toString());
              }
              sb.append(neType);
              sb.append("\n");
            }
          } else {
            sb.append(thisTerm.getForm());
            sb.append("\t");
            sb.append(thisTerm.getLemma());
            sb.append("\t");
            sb.append(thisTerm.getMorphofeat());
            sb.append("\t");
            sb.append(BIO.BEGIN.toString());
            sb.append(neType);
            sb.append("\n");
          }
          i += neSpanSize - 1;
        } else {
          sb.append(thisTerm.getForm());
          sb.append("\t");
          sb.append(thisTerm.getLemma());
          sb.append("\t");
          sb.append(thisTerm.getMorphofeat());
          sb.append("\t");
          sb.append(BIO.OUT);
          sb.append("\n");
        }
      }
      sb.append("\n");// end of sentence
    }
    return sb.toString();
  }

  /**
   * Convert Entity class annotation to CoNLL formats.
   * 
   * @param neType
   *          named entity class
   * @return the converted string
   */
  public String convertToConLLTypes(String neType) {
    String conllType = null;
    if (neType.equalsIgnoreCase("PERSON")
        || neType.equalsIgnoreCase("ORGANIZATION")
        || neType.equalsIgnoreCase("LOCATION") || neType.length() == 3) {
      conllType = neType.substring(0, 3);
    } else {
      conllType = neType;
    }
    return conllType;
  }

}
