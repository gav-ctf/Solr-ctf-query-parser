/* COPYRIGHT (C) 2015 Gavin Ruddy. All Rights Reserved. */

/**
 * Calls preparatory queries in ctfBase or ctfClickBase, adds 2y items to query string, parses query & calls ctfScorer.
 * Not for distribution
 * @author Gavin Ruddy
 * contact gav@pontneo.com
 * @version 1.0.1 2015/8/01
 */

package com.ctf;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.ArrayList;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import static org.apache.solr.search.QParser.getParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SyntaxError;

public class ctfQParser extends QParserPlugin {

  private static long CSstarttime;
  private static NamedList<String> ctfParams = new NamedList<String>();
  private static ArrayList<String> primary = new ArrayList<String>();
  private static ArrayList<String> secondary = new ArrayList<String>();
  private static Integer period = 1000;

  @SuppressWarnings("unchecked")
  public void init(NamedList args) {
    ctfParams.addAll(args);
    SolrParams params = SolrParams.toSolrParams(args);
  }

  public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {

    String base = localParams.get("base", params.get("base", (String) ctfParams.get("base")));
    boolean restrict = Boolean.valueOf(localParams.get("restrict", params.get("restrict", (String) ctfParams.get("restrict"))));
    boolean extend = Boolean.valueOf(localParams.get("extend", params.get("extend", (String) ctfParams.get("extend"))));
    boolean only2y = Boolean.valueOf(localParams.get("only2y", params.get("only2y", (String) ctfParams.get("only2y"))));
    String improvements = "off";
    if ( req.getParams().get("improvements") != null ) {
      improvements = req.getParams().get("improvements");
    }
    ctfDataHandler dh = new ctfDataHandler();
    if (base.equals("matches")) {
      ctfBase ctfbase = new ctfBase();
      primary = ctfbase.initialScores(qstr,localParams,params,req,ctfParams);
      period = ctfbase.getPeriod(primary);
      secondary = ctfbase.clickSampler(primary,period,localParams,params,req);
      if ( improvements.equals("on") && !only2y ) {
        String done = ctfbase.getInitialPosns(qstr,localParams,params,req);
      }
    }
    else {
      ctfClickBase ctfbase = new ctfClickBase();
      period = ctfbase.getPeriod(qstr,localParams,params,req,ctfParams);
      primary = ctfbase.clickSampler(period,qstr,localParams,params,req);
      secondary = ctfbase.findSecondary(primary,period,localParams,params,req);
      if ( improvements.equals("on") && !only2y ) {
        String done = ctfbase.getInitialPosns(qstr,localParams,params,req);
      }
    }
    String primaryObjs = String.join(",",primary);
    String secondaryObjs = String.join(",",secondary);
    if ( secondary.size() > 0 ){
      if ( !only2y ) {
        qstr = "((" + qstr + ") ({!terms f="+dh.getDOCID()+"}" + secondaryObjs + "))";
      }
      else {
        qstr = "(({!terms f="+dh.getDOCID()+"}-1) ({!terms f="+dh.getDOCID()+"}"+secondaryObjs+"))";
        dh.primaryObjClear();
      }
    }
    else {
      qstr = "((" + qstr + ") ({!terms f="+dh.getDOCID()+"}-1))";
    }
    if ( restrict ) {
      NamedList<Object> adjParams = req.getParams().toNamedList();
      String rfq = "({!terms f="+dh.getDOCID()+"}"+primaryObjs+") ({!terms f="+dh.getDOCID()+"}"+secondaryObjs+")";
      adjParams.add(CommonParams.FQ, rfq );
      req.setParams(SolrParams.toSolrParams(adjParams));
    }
    CSstarttime = System.currentTimeMillis();
    return new ctfParser(qstr, localParams, params, req);
  }

  private static class ctfParser extends QParser {
    private Query innerQuery;

    public ctfParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
      super(qstr, localParams, params, req);
      try {
        QParser parser = getParser(qstr, "lucene", getReq());
        this.innerQuery = parser.parse();
      } catch (SyntaxError ex) {
        throw new RuntimeException("error parsing query", ex);
      }
      ctfDataHandler dh = new ctfDataHandler();
      dh.addCSQtime( System.currentTimeMillis() - CSstarttime );
    }

    public Query parse() throws SyntaxError {
        return new ctfScorer(innerQuery);
    }
  }
}