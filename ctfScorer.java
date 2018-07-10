/* COPYRIGHT (C) 2015 Gavin Ruddy. All Rights Reserved. */

/**
 * Document scorer using click data from queries in ctfBse, stored in ctfDataHandler.
 * Not for distribution
 * @author Gavin Ruddy
 * contact gav@pontneo.com
 * @version 1.0.1 2015/8/01
 */

package com.ctf;

import java.io.IOException;
import org.apache.lucene.search.Query;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.document.Document;

public class ctfScorer extends CustomScoreQuery {

  public ctfScorer(Query subQuery) {
    super(subQuery);
    //this.setStrict(true);
  }

  protected CustomScoreProvider getCustomScoreProvider(LeafReaderContext context) throws IOException {
      return new ctfScoreProvider(context);
  }

  class ctfScoreProvider extends CustomScoreProvider {

    ctfDataHandler dh = new ctfDataHandler();

    public ctfScoreProvider(LeafReaderContext context) {
      super(context);
    }

    public float customScore(int doc, float subQueryScore, float valSrcScore) throws IOException {
      return customScore(doc, subQueryScore, new float[]{valSrcScore});
    }

    public float customScore(int doc, float subQueryScore, float[] valSrcScores) throws IOException {
      Document d = context.reader().document(doc);
      String docID = (d.get(dh.getDOCID())).toString();
      float docScore = subQueryScore;
      for (float valSrcScore : valSrcScores) {
          docScore *= valSrcScore;
      }
      float score = docScore;
      float tieBreak = dh.getREORDER() ? 1.00001f : 1f;
      if ( dh.getPrimaryObj().indexOf(docID) > -1 ) {
        score = dh.getClickBoost(docID) * docScore * tieBreak;
      }
      else if ( dh.getSecondaryObj().indexOf(docID) > -1 ) {
        score = dh.getClickBoost(docID) * dh.getInitDocScores( dh.getParent(docID) );
      }
      dh.addDocScore(docID,score);
      return score;
    }

  }
}