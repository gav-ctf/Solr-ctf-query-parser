/* COPYRIGHT (C) 2015 Gavin Ruddy. All Rights Reserved. */

/**
 * Queries for 1y items from top text matches, time integration, clicks and 2y items, then sends data to ctfDataHandler.
 * @author Gavin Ruddy
 * contact gav@pontneo.com
 * @version 1.1.0 2016/01/29
 */

package com.ctf;

import java.io.IOException;
import java.util.Iterator;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.client.solrj.response.FacetField;

public class ctfBase {

  private static String ctfhost;
  private static String ctf_docs_core;
  private static String ctf_clicks_core;
  private static String ctf_null_click;
  private static Boolean extend;
  private static String ctf_docID;
  private static String ctf_fromID;
  private static String ctf_toID;
  private static String ctf_time_stamp;
  private static String cn;
  private static Double cd;
  private static String cp;
  private static Double cs;
  private static Double cg;
  private static String cf;
  private static String ctp;
  private static String cts;
  private static String cz;
  private static ctfDataHandler dh = new ctfDataHandler();

  public ArrayList<String> initialScores(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, NamedList ctfParams) {

    cn = localParams.get("cn", params.get("cn", (String) ctfParams.get("cn")));
    cs = Double.parseDouble(localParams.get("cs", params.get("cs", (String) ctfParams.get("cs"))));
    cg = Double.parseDouble(localParams.get("cg", params.get("cg", (String) ctfParams.get("cg"))));
    cd = Double.parseDouble(localParams.get("cd", params.get("cd", (String) ctfParams.get("cd"))));
    cp = localParams.get("cp", params.get("cp", (String) ctfParams.get("cp")));
    cf = localParams.get("cf", params.get("cf", (String) ctfParams.get("cf")));
    ctp = localParams.get("ctp", params.get("ctp", (String) ctfParams.get("ctp")));
    cts = localParams.get("cts", params.get("cts", (String) ctfParams.get("cts")));
    cz = localParams.get("cz", params.get("cz", (String) ctfParams.get("cz")));
    extend = Boolean.valueOf(localParams.get("extend", params.get("extend", (String) ctfParams.get("extend"))));
    ctfhost = (String) ctfParams.get("solr_host_url");
    ctf_docs_core = (String) ctfParams.get("document_core_to_query");
    ctf_clicks_core = (String) ctfParams.get("clicks_core_to_query");
    ctf_null_click = (String) ctfParams.get("click_null_fromID_value");

    try {
      dh.reset(localParams,params,ctfParams);
      String url = ctfhost;
      SolrClient solr = new HttpSolrClient(url);
      SolrQuery query = new SolrQuery();
      String core = ctf_docs_core;
      query.setQuery( "(("+qstr+") ({!terms f="+dh.getDOCID()+"}-1))" );
      String[] fqs = params.getParams(CommonParams.FQ);
      if (fqs!=null && fqs.length!=0) {
        for (String fq : fqs) {
          if (fq != null && fq.trim().length()!=0) {
            query.addFilterQuery( fq );
          }
        }
      }
      if ( ctp != "*" ) {
        String ctpFQ = "({!join from="+dh.getTOID()+" to="+dh.getDOCID()+" fromIndex="+ctf_clicks_core+" v=$qqq} {!join from="+dh.getFROMID()+" to="+dh.getDOCID()+" fromIndex="+ctf_clicks_core+" v=$qqq})";
        query.set("qqq",ctp);
        query.addFilterQuery( ctpFQ );
      }
      query.set("defType","lucene");
      query.setRequestHandler("/select");
      query.set("fl",dh.getDOCID()+",score");
      query.set("rows",cn);
      query.set("sort","score desc");
      query.setIncludeScore(true);
      QueryResponse iSresponse = solr.query(core, query);
      SolrDocumentList docs = iSresponse.getResults();
      long numDocs = docs.getNumFound();
      Integer num1yFound = (int) (long) numDocs;
      dh.addQnum1yFound(num1yFound);
      Iterator<SolrDocument> dociterator = docs.iterator();
      int i = 0;
      while (dociterator.hasNext()){
        i++;
        SolrDocument doc = dociterator.next();
        String docID = (doc.getFirstValue(dh.getDOCID())).toString();
        float score = (float) (doc.getFirstValue("score"));
        dh.addPrimaryObj(docID);
        dh.addInitDocScores(docID,score);
      }

      } catch (IOException ex) {
        //
      } catch (SolrServerException ex) {
        throw new RuntimeException("Problem with SolrQuery connection",ex);
      }
      return dh.getPrimaryObj();

  }

  public Integer getPeriod(ArrayList<String> primaryObjs) {

    int period = 1000;
    try {
      String qq = String.join(",",primaryObjs );
      String url = ctfhost;
      SolrClient solr = new HttpSolrClient(url);
      SolrQuery query = new SolrQuery();
      String core = ctf_clicks_core;
      query.setQuery("({!terms f="+dh.getTOID()+" v=$qq} {!terms f="+dh.getFROMID()+" v=$qq})");
      query.set("qq",qq);
      query.set("defType","lucene");
      query.setRequestHandler("/select");
      query.set("fl","mins_ago:div(ms("+cz+","+dh.getTIMESTAMP()+"),6e4)");
      query.set("rows",cp);
      query.set("sort",dh.getTIMESTAMP()+" desc");
      if ( ctp != "*" ) {
        query.addFilterQuery( ctp );
      }
      if ( cz != "NOW" ) {
        query.addFilterQuery( dh.getTIMESTAMP() + ":[* TO " + cz + "]" );
      }
      QueryResponse Presponse = solr.query(core, query);
      SolrDocumentList docs = Presponse.getResults();
      if ( docs.size() > 0 ) {
        SolrDocument doc = docs.get(docs.size()-1);
        float mins = (float) (doc.getFirstValue("mins_ago"));
        int numFound = Math.min(Integer.parseInt(cp),(int)docs.getNumFound());
        period = (int) (Math.round(cd * Integer.parseInt(cn)/numFound * mins));
      }
      dh.addQperiod(period);
    } catch (IOException ex) {
      //
    } catch (SolrServerException ex) {
      throw new RuntimeException("Problem with SolrQuery connection",ex);
    }
    return dh.getQperiod();
  }

  public ArrayList<String> clickSampler(ArrayList<String> primaryObjs, Integer period, SolrParams localParams, SolrParams params, SolrQueryRequest req) {

    try {
      String url = ctfhost;
      SolrClient solr = new HttpSolrClient(url);
      String qq = String.join(",",primaryObjs );
      SolrQuery query = new SolrQuery();
      String core = ctf_clicks_core;
      query.setQuery("({!terms f="+dh.getTOID()+" v=$qq} {!terms f="+dh.getFROMID()+" v=$qq})");
      query.set("qq",qq);
      query.set("defType","lucene");
      query.setRequestHandler("/select");
      query.set("rows","0");
      query.addFilterQuery( dh.getTIMESTAMP()+":[" + cz + "-" + period + "MINUTE TO " + cz + "]");
      if ( ctp != "*" ) {
        query.addFilterQuery( ctp );
      }
      query.set("facet","true");
      query.addFacetField(dh.getTOID());
      query.addFacetField(dh.getFROMID());
      query.set("facet.mincount","1");
      query.set("facet.limit","-1");
      query.set("facet.sort","count");
      QueryResponse cRresponse = solr.query(core, query);
      List<FacetField> fflist = cRresponse.getFacetFields();
      if (fflist != null) {
        for(FacetField ff : fflist){
          List<FacetField.Count> counts = ff.getValues();
          if (counts != null) {
            for(FacetField.Count c : counts){
              String cdocID = c.getName();
              if ( primaryObjs.indexOf(cdocID) > -1 ) {
                int clicks = (int)c.getCount();
                dh.addClicks(cdocID, clicks);
              }
            }
          }
        }
      }
    } catch (IOException ex) {
      //
    } catch (SolrServerException ex) {
      throw new RuntimeException("Problem with SolrQuery connection",ex);
    }

    try {
      String url = ctfhost;
      SolrClient solr = new HttpSolrClient(url);
      String qq = String.join(",",primaryObjs );
      SolrQuery query = new SolrQuery();
      String core = ctf_clicks_core;
      query.setQuery("({!terms f="+dh.getTOID()+" v=$qq} {!terms f="+dh.getFROMID()+" v=$qq})");
      query.set("qq",qq);
      query.set("defType","lucene");
      query.setRequestHandler("/select");
      query.set("fl",dh.getFROMID()+","+dh.getTOID());
      query.set("sort",dh.getTIMESTAMP()+" desc");
      query.set("rows",String.valueOf( 10 * Math.round(Float.parseFloat( cn ) * cd) ));
      query.addFilterQuery( dh.getTIMESTAMP()+":[" + cz + "-" + period + "MINUTE TO " + cz + "]");
      if ( cts != "*" ) {
        query.addFilterQuery( cts );
      }
      query.set("facet","true");
      query.addFacetField(dh.getTOID());
      query.addFacetField(dh.getFROMID());
      query.set("facet.mincount","1");
      query.set("facet.limit","-1");
      query.set("facet.sort","count");
      QueryResponse cSresponse = solr.query(core, query);
      SolrDocumentList docs = cSresponse.getResults();
      if ( extend ) {
        Iterator<SolrDocument> dociterator = docs.iterator();
        while (dociterator.hasNext()){
          SolrDocument doc = dociterator.next();
          String fromID = (doc.getFirstValue(dh.getFROMID())).toString();
          String toID = (doc.getFirstValue(dh.getTOID())).toString();
          if ( primaryObjs.indexOf(toID) > -1 && primaryObjs.indexOf(fromID) < 0 ) {
            dh.addSecondaryObj(fromID);
            dh.addParent(fromID, toID);
          }
          else if ( primaryObjs.indexOf(fromID) > -1 && primaryObjs.indexOf(toID) < 0 ) {
            dh.addSecondaryObj(toID);
            dh.addParent(toID, fromID);
          }
        }
      }
      List<FacetField> fflist = cSresponse.getFacetFields();
      if (fflist != null) {
        for(FacetField ff : fflist){
          List<FacetField.Count> counts = ff.getValues();
          if (counts != null) {
            for(FacetField.Count c : counts){
              String cdocID = c.getName();
              if ( dh.getSecondaryObj().indexOf(cdocID) > -1 ) {
                int clicks = (int)c.getCount();
                dh.addClicks(cdocID, clicks);
              }
            }
          }
        }
      }
    } catch (IOException ex) {
      //
    } catch (SolrServerException ex) {
      throw new RuntimeException("Problem with SolrQuery connection",ex);
    }

    if ( extend ) {

      getSumClicksSecondary( filterSecondary( detailFilter( tidySecondary( dh.getSecondaryObj() ), dh.getTotalSecondaryClicks()), localParams,params,req ) );

    }
    else if ( !extend ) {
      dh.parentsClear();
      dh.secondaryObjClear();
    }
    dh.normalise();
    return dh.getSecondaryObj();
  }

  public ArrayList<String> filterSecondary( ArrayList<String> secondary, SolrParams localParams, SolrParams params, SolrQueryRequest req ) {

    try {
      String url = ctfhost;
      SolrClient solr = new HttpSolrClient(url);
      SolrQuery query = new SolrQuery();
      String core = ctf_docs_core;
      query.setQuery( "{!terms f="+dh.getDOCID()+" v=$qq}" );
      query.set("qq",String.join(",",secondary));
      query.addFilterQuery( cf );
      String[] fqs = params.getParams(CommonParams.FQ);
      if (fqs!=null && fqs.length!=0) {
        for (String fq : fqs) {
          if (fq != null && fq.trim().length()!=0) {
            query.addFilterQuery( fq );
          }
        }
      }
      query.set("defType","lucene");
      query.setRequestHandler("/select");
      query.set("fl",dh.getDOCID());
      query.set("rows",secondary.size());
      dh.secondaryObjClear();
      QueryResponse sFresponse = solr.query(core, query);
      SolrDocumentList docs = sFresponse.getResults();
      Iterator<SolrDocument> dociterator = docs.iterator();
      int numFound = (int)docs.getNumFound();
      dh.putTotalDocs(numFound);
      while (dociterator.hasNext()){
        SolrDocument doc = dociterator.next();
        String docID = (doc.getFirstValue(dh.getDOCID())).toString();
        dh.addSecondaryObj(docID);
      }

      } catch (IOException ex) {
        //
      } catch (SolrServerException ex) {
        throw new RuntimeException("Problem with SolrQuery connection",ex);
      }
    return dh.getSecondaryObj();
  }

  public ArrayList<String> detailFilter(Map<String, Integer> map, int total) {
    int cumulative = 0;
    int cutoff = (int) (cs * total);
    for (Map.Entry<String, Integer> entry : map.entrySet()) {
      String docID = entry.getKey();
      int clicks = entry.getValue();
      cumulative += clicks;
      if (  cumulative > cutoff ) {
        dh.removeSecondaryObj(docID);
        dh.removeClicks(docID);
      }
    }
    return dh.getSecondaryObj();
  }

  public Map<String,Integer> tidySecondary(ArrayList<String> secondary) {
      int totalSecondaryClicks = 0;
      for (String docID : secondary) {
        if ( dh.getParent(docID).equals(docID) || dh.getSecondaryClicks(docID) < 1 ) {
          dh.removeSecondaryObj(docID);
          dh.removeClicks(docID);
        }
        else {
          totalSecondaryClicks += dh.getClicks(docID);
        }
      }
      dh.setTotalSecondaryClicks(totalSecondaryClicks);
    return dh.getSecondaryClicks();
  }

  public String getInitialPosns(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {

    try {
      String url = ctfhost;
      SolrClient solr = new HttpSolrClient(url);
      SolrQuery query = new SolrQuery();
      String core = ctf_docs_core;
      query.setQuery( qstr );
      query.set("defType","lucene");
      query.setRequestHandler("/select");
      query.set("fl",dh.getDOCID());
      query.set("rows","1000");
      String[] fqs = params.getParams(CommonParams.FQ);
      if (fqs!=null && fqs.length!=0) {
        for (String fq : fqs) {
          if (fq != null && fq.trim().length()!=0) {
            query.addFilterQuery( fq );
          }
        }
      }
      QueryResponse cFresponse = solr.query(core, query);
      SolrDocumentList docs = cFresponse.getResults();
      Iterator<SolrDocument> dociterator = docs.iterator();
      int i = 0;
      while (dociterator.hasNext()){
        i++;
        SolrDocument doc = dociterator.next();
        String docID = (doc.getFirstValue(dh.getDOCID())).toString();
        if ( dh.getPrimaryObj().indexOf(docID) > -1 || dh.getSecondaryObj().indexOf(docID) > -1 ) {
          if ( dh.getClicks(docID) < 1 ) {
            dh.removePrimaryObj(docID);
            dh.removeSecondaryObj(docID);
          }
          else {
            dh.addInitPosns(docID,i);
          }
        }
      }
    } catch (IOException ex) {
      //
    } catch (SolrServerException ex) {
      throw new RuntimeException("Problem with SolrQuery connection",ex);
    }
    return "done";
  }

  public void getSumClicksSecondary( ArrayList<String> secondary ) {

    try {
      String url = ctfhost;
      SolrClient solr = new HttpSolrClient(url);
      SolrQuery query = new SolrQuery();
      String core = ctf_clicks_core;
      query.set("defType","lucene");
      query.setRequestHandler("/select");
      query.set("rows",0);
      query.setQuery("{!terms f="+dh.getTOID()+" v=$qq}");
      query.set("qq",String.join(",",secondary));
      query.addFilterQuery( dh.getTIMESTAMP()+":[" + cz + "-" + dh.getQperiod() + "MINUTE TO " + cz + "]");
      if ( cts != "*" ) {
        query.addFilterQuery( cts );
      }
      query.set("facet","true");
      query.set("facet","true");
      query.addFacetField(dh.getTOID());
      query.set("facet.mincount","1");
      query.set("facet.limit","-1");
      QueryResponse cZresponse = solr.query(core, query);
      SolrDocumentList docs = cZresponse.getResults();
      List<FacetField> fflist = cZresponse.getFacetFields();
      if (fflist != null) {
        for(FacetField ff : fflist){
          List<FacetField.Count> counts = ff.getValues();
          if (counts != null) {
            for(FacetField.Count c : counts){
              String cdocID = c.getName();
              int clicks = (int)c.getCount();
              dh.addSClicks(cdocID, clicks);
            }
          }
        }
      }
    } catch (IOException ex) {
      //
    } catch (SolrServerException ex) {
      throw new RuntimeException("Problem with SolrQuery connection",ex);
    }

    try {
      String url = ctfhost;
      SolrClient solr = new HttpSolrClient(url);
      SolrQuery query = new SolrQuery();
      String core = ctf_clicks_core;
      query.set("defType","lucene");
      query.setRequestHandler("/select");
      query.set("rows",0);
      query.setQuery("{!terms f="+dh.getFROMID()+" v=$qq}");
      query.set("qq",String.join(",",secondary));
      query.addFilterQuery( dh.getTIMESTAMP()+":[" + cz + "-" + dh.getQperiod() + "MINUTE TO " + cz + "]");
      if ( cts != "*" ) {
        query.addFilterQuery( cts );
      }
      query.set("facet","true");
      query.addFacetField(dh.getFROMID());
      query.set("facet.mincount","1");
      query.set("facet.limit","-1");
      QueryResponse cYresponse = solr.query(core, query);
      SolrDocumentList docs = cYresponse.getResults();
      List<FacetField> fflist = cYresponse.getFacetFields();
      if (fflist != null) {
        for(FacetField ff : fflist){
          List<FacetField.Count> counts = ff.getValues();
          if (counts != null) {
            for(FacetField.Count c : counts){
              String cdocID = c.getName();
              int clicks = (int)c.getCount();
              dh.addSClicks(cdocID, clicks);
            }
          }
        }
      }
    } catch (IOException ex) {
      //
    } catch (SolrServerException ex) {
      throw new RuntimeException("Problem with SolrQuery connection",ex);
    }

  }

}
