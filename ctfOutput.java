/* COPYRIGHT (C) 2015 Gavin Ruddy. All Rights Reserved. */

/**
 * Calculates and outputs query metrics from ctfBase, ctfScorer and ctfQParser.
 * @author Gavin Ruddy
 * contact gav@pontneo.com
 * @version 1.0.1 2015/8/01
 */

package com.ctf;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;

public class ctfOutput extends SearchComponent {

    private static float primaryImprovement = 0;
    private static float secondaryImprovement = 0;
    private static float netImprovement = 0;
    private static ctfDataHandler dh = new ctfDataHandler();

    @Override
    public void init(NamedList args) {
        super.init(args);
    }

    @Override
    public void prepare(ResponseBuilder rb) throws IOException {

    }

    @Override
    public void process(ResponseBuilder rb) throws IOException {

        SolrIndexSearcher searcher = rb.req.getSearcher();
        NamedList<String> response = new NamedList<String>();
        DocList docs = rb.getResults().docList;
        int numDocs = docs.matches();
        DocIterator iterator = docs.iterator();
        for (int i = 0; i < docs.size(); i++) {
            try {
                int val = iterator.nextDoc();
                Document d = searcher.doc(val);
                String docID = (d.get(dh.getDOCID())).toString();
                float score = iterator.score();
                String docParentID = dh.getParent(docID);
                String label = "";
                String details = "";
                if ( dh.getClicks(docID) > 0 ) {
                  if ( dh.getSecondaryObj().indexOf(docID) > -1 ) {
                    label += String.valueOf("2y item, parent:" + dh.getParent(docID) + ", ");
                    details = String.valueOf(label) + "traffic:" + String.valueOf(dh.getClicks(docID)) + ", niche:" + String.valueOf((float) dh.getClicks(docID)/dh.getSClicks(docID)) + ", boost:" + String.valueOf(dh.getClickBoost(docID)) + ", score:" + String.valueOf( score );
                  }
                  if ( dh.getPrimaryObj().indexOf(docID) > -1 ) {
                    label += "1y item, ";
                    details = String.valueOf(label) + "traffic:" + String.valueOf(dh.getClicks(docID)) + ", boost:" + String.valueOf(dh.getClickBoost(docID)) + ", score:" + String.valueOf( score );
                  }
                  if ( dh.getSecondaryObj().indexOf(docID) < 0 && dh.getPrimaryObj().indexOf(docID) < 0 ) {
                    label += "unaffected text match, ";
                    details = String.valueOf(label) + "score:" + String.valueOf( score );
                  }
                }
                else {
                    label += "unaffected text match, ";
                    details = String.valueOf(label) + "score:" + String.valueOf( score );
                }
                response.add(docID, details);
            } catch (IOException ex) {
            }
        }
        getImprovements(dh.getSortedDocScores());
        String impa = ( Float.isNaN(primaryImprovement) || Float.isInfinite(primaryImprovement) ) ? "-" : String.valueOf(primaryImprovement);
        String impb = ( Float.isNaN(netImprovement) || Float.isInfinite(netImprovement) ) ? "-" : String.valueOf(secondaryImprovement);
        String impc = ( Float.isNaN(netImprovement) || Float.isInfinite(netImprovement) ) ? "-" : String.valueOf(netImprovement);
        NamedList<String> qresults = new NamedList<String>();
        qresults.add("CTF prep time (ms)", String.valueOf(dh.getCTFQtime()));
        qresults.add("CTF parser time (ms)", String.valueOf(dh.getCSQtime()));
        qresults.add("num 1y items", String.valueOf(dh.getPrimaryObj().size()));
        qresults.add("num 2y items", String.valueOf(dh.getSecondaryObj().size()));
        qresults.add("items affected (%)", String.valueOf( 100 * ((float) dh.getPrimaryObj().size() + (float) dh.getSecondaryObj().size()) / numDocs ) );
        qresults.add("sample period (days)", String.valueOf((float) dh.getQperiod()/(60*24)));
        qresults.add("avg click density", String.valueOf((float) dh.getQClicksFound() /(float) (dh.getPrimaryObj().size()+dh.getSecondaryObj().size())));
        qresults.add("1y improvement (%)", impa );
        qresults.add("2y improvement (%)", impb );
        qresults.add("net improvement (%)", impc );
        rb.rsp.add("CTF output", qresults);
        rb.rsp.add("item details", response);

    }

    public static void getImprovements(Map<String, Float> map) {

      int count = 0;
      float adjImp = 0;
      float secImp = 0;
      float baseImp = 0;
      float baseSImp = 0;
      primaryImprovement = 0;
      secondaryImprovement = 0;
      netImprovement = 0;

      ctfDataHandler dh = new ctfDataHandler();
      for (Map.Entry<String, Float> entry : map.entrySet()) {
        count++;
        String docID = entry.getKey();
        float score = entry.getValue();
        int clicks = dh.getClicks(docID);
            //Double ctr = Math.exp(Math.log(1.55)*-1*count) * 1.0177*Math.pow(1.55/(1+Math.log(dh.getTotalDocs())),0.8769);
        Double ctr = 0.37317*Math.pow(count,-1.3264);
        if ( dh.getInitialPosns().containsKey(docID) ) {
          adjImp += ctr.floatValue() * score;
              //Double pctr = Math.exp(Math.log(1.55)*-1*(float) dh.getInitPosn(docID)) * 1.0177*Math.pow(1.55/(1+Math.log(dh.getTotalDocs())),0.8769);
          Double pctr = 0.37317*Math.pow(dh.getInitPosn(docID),-1.3264);
          baseImp += pctr.floatValue() * score;
          baseSImp += ctr.floatValue() * clicks;
        }
        else if ( dh.getSecondaryObj().indexOf(docID) > -1 ) {
          secImp += ctr.floatValue() * clicks;
        }
        else {
            adjImp += ctr.floatValue() * score;
            baseImp += ctr.floatValue() * score;
        }
      }
      primaryImprovement = ( !dh.getREORDER() && dh.getSecondaryObj().size() < 1 ) ? 0 : 100 * (adjImp /baseImp - 1);
      secondaryImprovement = 100 * secImp/baseSImp;
      netImprovement = primaryImprovement + secondaryImprovement;
    }

    @Override
    public String getDescription() {
        return "Outputs query metrics for ctfQParser";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

}
