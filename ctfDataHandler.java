/* COPYRIGHT (C) 2015 Gavin Ruddy. All Rights Reserved. */

/**
 * Stores and handles data from ctfBase for use in ctfQParser and ctfOutput.
 * Not for distribution
 * @author Gavin Ruddy
 * contact gav@pontneo.com
 * @version 1.1.0 2016/01/29
 */

package com.ctf;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

public class ctfDataHandler {

  private static HashMap<String,Float> docScores = new HashMap<String,Float>();
  private static HashMap<String,Float> initDocScores = new HashMap<String,Float>();
  private static HashMap<String,Integer> initPosns = new HashMap<String,Integer>();
  private static HashMap<String, Float> sortedDocScores = new HashMap<String,Float>();
  private static ArrayList<String> primaryObj = new ArrayList<String>();
  private static HashMap<String,Integer> clicks = new HashMap<String,Integer>();
  private static HashMap<String,Integer> secondaryClicks = new HashMap<String,Integer>();
  private static HashMap<String,Integer> secondarySClicks = new HashMap<String,Integer>();
  private static HashMap<String,String> parents = new HashMap<String,String>();
  private static ArrayList<String> secondaryObj = new ArrayList<String>();
  private static ArrayList<String> forRemoval = new ArrayList<String>();
  private static int maxPrimaryClicks = 0;
  private static int maxSecondaryClicks = 0;
  private static int maxSSecondaryClicks = 0;
  private static Double maxSpec = (double) 0;
  private static int totalSecondaryClicks = 0;
  private static int Qperiod = 1000;
  private static int Qnum1yFound = 0;
  private static int QClicksFound = 0;
  private static long CTFQtime;
  private static long CSQtime;
  private static long CTFstarttime;
  private static boolean extend;
  private static boolean reorder;
  private static String ctf_null_click;
  private static float cb;
  private static Double cx;
  private static Double cg;
  private static Double cy;
  private static String testString = "";
  private static int totalDocs = 0;

  private static String DOCID;
  private static String FROMID;
  private static String TOID;
  private static String TIMESTAMP;

  public void reset(SolrParams localParams, SolrParams params, NamedList ctfParams){

   docScores.clear();
   forRemoval.clear();
   sortedDocScores.clear();
   initDocScores.clear();
   initPosns.clear();
   primaryObj.clear();
   clicks.clear();
   secondaryClicks.clear();
   secondarySClicks.clear();
   parents.clear();
   secondaryObj.clear();
   maxSecondaryClicks = 1;
   maxSSecondaryClicks = 1;
   maxSpec = (double) 1e-20;
   totalSecondaryClicks = 0;
   maxPrimaryClicks = 1;
   Qperiod = 1000;
   Qnum1yFound = 0;
   QClicksFound = 0;
   CTFstarttime = System.currentTimeMillis();
   testString = "";
   totalDocs = 0;

   cb = Float.parseFloat(localParams.get("cb", params.get("cb", (String) ctfParams.get("cb"))));
   cx = Double.parseDouble(localParams.get("cx", params.get("cx", (String) ctfParams.get("cx"))));
   cg = Double.parseDouble(localParams.get("cg", params.get("cg", (String) ctfParams.get("cg"))));
   cy = Double.parseDouble(localParams.get("cy", params.get("cy", (String) ctfParams.get("cy"))));
   extend = Boolean.valueOf(localParams.get("extend", params.get("extend", (String) ctfParams.get("extend"))));
   reorder = Boolean.valueOf(localParams.get("reorder", params.get("reorder", (String) ctfParams.get("reorder"))));
   ctf_null_click = (String) ctfParams.get("click_null_fromID_value");

   DOCID = (String) ctfParams.get("document_ID_field_name");
   FROMID = (String) ctfParams.get("click_fromID_field_name");
   TOID = (String) ctfParams.get("click_toID_field_name");
   TIMESTAMP = (String) ctfParams.get("click_time_stamp_field_name");

  }

  public void normalise() {
    int totalClicks = 0;
    if (clicks.size() > 0) {
      for (Map.Entry<String, Integer> entry : clicks.entrySet()) {
        String docID = entry.getKey();
        int sclicks = entry.getValue();
        totalClicks += sclicks;
        if (!primaryObj.contains(docID)){
          if ( sclicks > 0 && sclicks > maxSecondaryClicks ) {
            maxSecondaryClicks = sclicks;
          }
        }
        else {
          if ( sclicks > 0 && sclicks > maxPrimaryClicks ) {
            maxPrimaryClicks = sclicks;
          }
        }
      }
    }
    addQClicksFound(totalClicks);
    CTFQtime = System.currentTimeMillis() - CTFstarttime;
    if (secondarySClicks.size() > 0) {
      for (Map.Entry<String, Integer> entry : secondarySClicks.entrySet()) {
        String docID = entry.getKey();
        int clicks = getClicks(docID);
        int sclicks = entry.getValue();
        float spec = (float) clicks/sclicks;
          if ( spec > 0 && spec > maxSpec ) {
            maxSpec = (double) spec;
          }
          if ( sclicks > 0 && sclicks > maxSSecondaryClicks ) {
            maxSSecondaryClicks = sclicks;
          }
      }
    }
  }

  public String getDOCID() {
    return DOCID;
  }

  public String getFROMID() {
    return FROMID;
  }

  public String getTOID() {
    return TOID;
  }

  public String getTIMESTAMP() {
    return TIMESTAMP;
  }

  public boolean getREORDER() {
    return reorder;
  }

  public void addPrimaryObj(String id) {
    if ( primaryObj.indexOf(id) < 0 ) {
      primaryObj.add(id);
    }
  }

  public void addSecondaryObj(String id) {
    if ( secondaryObj.indexOf(id) < 0 && primaryObj.indexOf(id) < 0 && !id.equals(ctf_null_click) ) {
      secondaryObj.add(id);
    }
  }

  public ArrayList<String> getPrimaryObj() {
    return primaryObj;
  }

  public ArrayList<String> getSecondaryObj() {
    return secondaryObj;
  }

  public void removePrimaryObj(String id) {
    if ( primaryObj.indexOf(id) > -1 ) {
      primaryObj.remove( primaryObj.indexOf(id) );
    }
  }

  public void removeSecondaryObj(String id) {
    if ( secondaryObj.indexOf(id) > -1 ) {
      secondaryObj.remove( secondaryObj.indexOf(id) );
    }
  }

  public void secondaryObjClear() {
      secondaryObj.clear();
  }

  public void primaryObjClear() {
      primaryObj.clear();
  }

  public void removeClicks(String id) {
    if ( clicks.containsKey(id) ) {
      clicks.remove(id);
    }
    if ( secondaryClicks.containsKey(id) ) {
      secondaryClicks.remove( id );
    }
  }

  public void addClicks(String id, int count) {
    if ( extend ) {
      if ( primaryObj.indexOf(id) > -1 || secondaryObj.indexOf(id) > -1 ) {
        if ( clicks.containsKey(id) ){
          clicks.put(id, clicks.get(id) + count);
        }
        else {
          clicks.put(id, count);
        }
      }
      if ( secondaryObj.indexOf(id) > -1 ) {
        if ( secondaryClicks.containsKey(id) ){
          secondaryClicks.put(id, secondaryClicks.get(id) + count);
        }
        else {
          secondaryClicks.put(id, count);
        }
      }
    }
    else {
      if ( primaryObj.indexOf(id) > -1 ) {
        if ( clicks.containsKey(id) ){
          clicks.put(id, clicks.get(id) + count);
        }
        else {
          clicks.put(id, count);
        }
      }
    }
  }

  public void addSClicks(String id, int count) {
    if ( secondarySClicks.containsKey(id) ){
      secondarySClicks.put(id, secondarySClicks.get(id) + count);
    }
    else {
      secondarySClicks.put(id, count);
    }
  }

  public HashMap<String,Integer> getClicks() {
    HashMap<String, Integer> sortedClicks = sortByInt(clicks);
    return sortedClicks;
  }

  public HashMap<String,Integer> getSecondaryClicks() {
    HashMap<String, Integer> sortedClicks = sortByInt(secondaryClicks);
    return sortedClicks;
  }

  public int getClicks(String id) {
    int docclicks = 0;
    if ( clicks.containsKey(id) ){
      docclicks = clicks.get(id);
    }
    return docclicks;
  }

  public int getSecondaryClicks(String id) {
    int docclicks = 0;
    if ( secondaryClicks.containsKey(id) ){
      docclicks = secondaryClicks.get(id);
    }
    return docclicks;
  }

  public int getSClicks(String id) {
    int docclicks = 0;
    if ( secondarySClicks.containsKey(id) ){
      docclicks = secondarySClicks.get(id);
    }
    return docclicks;
  }

  public int getTotalSecondaryClicks() {
    return totalSecondaryClicks;
  }

  public void setTotalSecondaryClicks(int total) {
    totalSecondaryClicks = total;
  }

  public float getClickBoost(String id) {
    float clickBoost = 1;
      if ( reorder && clicks.containsKey(id) ) {
        float c = (float) clicks.get(id);
        if ( secondaryObj.indexOf(id) > -1 ) {
          String pid = getParent(id);
          float pc = c;
          if ( clicks.containsKey(pid) ) {
            pc = (float) clicks.get(pid);
          }
          float sp = 1;
          if (secondarySClicks.containsKey(id) ) {
            sp = (float) c/secondarySClicks.get(id);
          }
          clickBoost += cb * Math.pow(cg * sp/maxSpec + (1-cg) * c/maxSecondaryClicks, cy) * Math.pow(0.9999 * pc/maxPrimaryClicks, cx);
        }
        else if ( primaryObj.indexOf(id) > -1 ) {
          clickBoost += cb * Math.pow(c/maxPrimaryClicks, cx);
        }
      }
    return clickBoost;
  }

  public String getParent(String id) {
    String parent = id;
    if ( parents.containsKey(id) ){
      parent = parents.get(id);
    }
    return parent;
  }

  public boolean checkParent(String id) {
    boolean parent = false;
    if ( parents.containsKey(id) ){
      String pid = parents.get(id);
      if ( primaryObj.indexOf(pid) > -1 && clicks.containsKey(pid)) {
        parent = true;
      }
    }
    return parent;
  }

  public void addParent(String id, String pid) {
    if ( !parents.containsKey(id) ){
      parents.put(id, pid);
    }
  }

  public void parentsClear() {
      parents.clear();
  }

  public void addDocScore(String id, float score) {
    docScores.put(id, score);
  }

  public float getDocScore(String id) {
    float score = 0;
    if ( docScores.containsKey(id) ){
      score = docScores.get(id);
    }
    return score;
  }

  public void addInitDocScores(String id, float score) {
    initDocScores.put(id, score);
  }

  public void addInitPosns(String id, int posn) {
    initPosns.put(id, posn);
  }

  public float getInitDocScores(String id) {
    float score = 1;
    if ( initDocScores.containsKey(id) ){
      score = initDocScores.get(id);
    }
    return score;
  }

  public String getTestString() {
    return testString;
  }

  public void putTestString(String str) {
    testString += str;
  }

  public int getInitPosn(String id) {
    int posn = 1000;
    if ( initPosns.containsKey(id) ){
        posn = initPosns.get(id);
    }
    return posn;
  }

  public HashMap<String,Integer> getInitialPosns() {
    return initPosns;
  }

  public HashMap<String,Float> getDocScores() {
    return docScores;
  }

  public HashMap<String,Float> getSortedDocScores() {
    HashMap<String, Float> sortedScores = sortByFloat(docScores);
    return sortedScores;
  }

  public int getMaxSecondaryClicks() {
    return maxSecondaryClicks;
  }

  public int getMaxSSecondaryClicks() {
    return maxSSecondaryClicks;
  }

  public void addQperiod(Integer period) {
    Qperiod = period;
  }

  public Integer getQperiod() {
    return Qperiod;
  }

  public void addQnum1yFound(Integer numFound) {
    Qnum1yFound = numFound;
  }

  public Integer getQnum1yFound() {
    return Qnum1yFound;
  }

  public void addQClicksFound(Integer numFound) {
    QClicksFound += numFound;
  }

  public Integer getQClicksFound() {
    return QClicksFound;
  }

  public long getCTFQtime() {
    return CTFQtime;
  }

  public long getCSQtime() {
    return CSQtime;
  }

  public void addCSQtime(long time) {
    CSQtime = time;
  }

  public void putTotalDocs(int total) {
    totalDocs = total;
  }

  public int getTotalDocs() {
    return totalDocs;
  }

  private static HashMap<String, Integer> sortByInt(Map<String, Integer> unsortMap) {
    List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
      public int compare(Map.Entry<String, Integer> o1,Map.Entry<String, Integer> o2) {
        return (o2.getValue()).compareTo(o1.getValue());
      }
    });

    HashMap<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
    for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {
      Map.Entry<String, Integer> entry = it.next();
      sortedMap.put(entry.getKey(), entry.getValue());
    }
    return sortedMap;
  }

  private static HashMap<String, Float> sortByFloat(Map<String, Float> unsortMap) {
    List<Map.Entry<String, Float>> list = new LinkedList<Map.Entry<String, Float>>(unsortMap.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {
      public int compare(Map.Entry<String, Float> o1,Map.Entry<String, Float> o2) {
        return (o2.getValue()).compareTo(o1.getValue());
      }
    });

    HashMap<String, Float> sortedMap = new LinkedHashMap<String, Float>();
    for (Iterator<Map.Entry<String, Float>> it = list.iterator(); it.hasNext();) {
      Map.Entry<String, Float> entry = it.next();
      sortedMap.put(entry.getKey(), entry.getValue());
    }
    return sortedMap;
  }
}
