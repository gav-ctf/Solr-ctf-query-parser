# Solr-ctf-query-parser
Reranks and expands Solr query returns using clickstream data. Clickthroughfilter.jar runs the filter as a query parser plugin for Solr/Lucene.

The filter samples click data for items returned by a query and uses it to

<table>
<tr><td>1.</td><td>reorganise</td><td>boost items in proportion to their click traffic (primary items)</td></tr>
<tr><td>2.</td><td>extend</td><td>inject new items not matching the query, but connected to the query's primary items by click traffic (secondary items)</td></tr>
<tr><td>3.</td><td>customise</td><td>boost & inject selected item types and boost & inject using selected components of click traffic</td></tr>
</table>

These elements can be used separately or in combination for improving search returns, current awareness, identifying related material, making personal recommendations etc (see example CTF queries below).

To minimise processing times, the filter acts either on the top n items of a query return ("base=matches": faster, but lower improvement), or looks deeper into the return and draws out the top n items in terms of click traffic ("base=clicks": potentially slower, but with potentially higher improvement).

For the most part, the filter can be built straight into many types of complex queries, including conjunction with other parsers, methods, facets etc. Where you find that a CTF query is not directly compatible with other complex queries (e.g. certain joins & group functions), you can usually find a way round by rearranging your input query or writing your own request handler.

A "/ctf" request handler is provided to help with testing and tuning (see attached solrconfig.xml). This quantifies primary and secondary improvements relative to the unmodified return and provides other useful metrics.

For more detail about the CTF process go to https://www.slideshare.net/pontneo/better-search-implementation-of-click-through-filter-as-a-query-parser-plugin-for-apache-solr-lucene

SOME EXAMPLE CTF QUERIES (parameters are described below):
---------------------------------------------------------
<table>
<tr><td>1.</td><td> search in text field of your documents core for "melon" at your default CTF settings;</td></tr>
<tr><td></td><td>	q={!ctf}text:melon or q={!ctf v=$qq} with qq=text:melon</td></tr>
<tr><td>2.</td><td> weighted search for "melon" in title or body fields using only "botanist" click traffic but otherwise default settings;</td></tr>
<tr><td></td><td>	q={!ctf ctp="user:botanist" cts="user:botanist" v=$qq} with qq=title:melon^2 body:melon</td></tr>
<tr><td>3.</td><td> weighted search for "melon" or "cherry" blending click traffic & publication_date boosting;</td></tr>
<tr><td></td><td>	q={!boost b=recip(ms(NOW/HOUR,publication_date),3.16e-11,1,1)}{!ctf cb=5 cx=2 v=$qq} with qq=text:melon^3 text:cherry^2
<tr><td></td><td>	or q={!ctf cb=5 cx=2 v=$qq} with qq=({!boost b=recip(ms(NOW/HOUR,publication_date),3.16e-11,1,1)}body:melon^3 {!boost b=recip(ms(NOW/HOUR,publication_date),3.16e-11,1,1)}body:cherry^2) etc</td></tr>
<tr><td>4.</td><td> filtered search for "orange" with all returned items in category fruit;</td></tr>
<tr><td></td><td>	q={!ctf v=$qq} with qq=text:orange and fq=category:fruit</td></tr>
<tr><td>5.</td><td> filtered search for "orange" with just secondary items in category fruit;</td></tr>
<tr><td></td><td>	q={!ctf cf="category:fruit" v=$qq} with qq=text:orange</td></tr>
<tr><td>6.</td><td> filtered search for "orange" with just primary items in category fruit;</td></tr>
<tr><td></td><td>	q={!ctf v=$qq} with qq=+text:orange +category:fruit</td></tr>
<tr><td>7.</td><td> search for "lychee" with a high sensitivity to changes over time, recoiling quickly to the unmodified search return;</td></tr>
<tr><td></td><td>	q={!ctf cp=5 cd=2 ctp="time_stamp:[NOW-1DAY TO NOW]" v=$qq} with qq=text:lychee</td></tr>
<tr><td>8.</td><td> search for "lychee" in title field preserving original sort & including best 10% of secondary items with title word like "lychee";</td></tr>
<tr><td></td><td>	q={!ctf reorder=false cf="title:lychee~0.5" cs=0.1 v=$qq} with qq=title:lychee</td></tr>
<tr><td>9.</td><td> last week's search for "kiwi" based on the 10 most clicked "kiwi" items, with the best 20% of non-pdf secondary items that are then pushed down the list to help maximise improvement;</td></tr>
<tr><td></td><td>	q={!ctf base=clicks cn=10 cz="NOW-7DAY" cs=0.2 cf="-doctype:pdf" cy=0.95 v=$qq} with qq=text:kiwi</td></tr>
<tr><td>10.</td><td> top 5 most visited "lemon" recipes by user types 2 & 3 over the last 7 days;</td></tr>
<tr><td></td><td>	q={!ctf cn=5 base=clicks restrict=true extend=false ctp="+user_type:(2 3) +time_stamp:[NOW-7DAY/DAY TO NOW]" v=$qq} with qq=recipe:*lemon*</td></tr>
<tr><td>11.</td><td> top 10 recommendations for userID:x, based on userID:x's up to 20 most visited items over the last month and click traffic through those items by any other user with an interest in "fruit" since userID:x's last visit;</td></tr>
<tr><td></td><td>	q={!ctf base=clicks only2y=true cn=20 ctp="userID:x AND time_stamp:[NOW-31DAY TO NOW]" cts="-userID:x AND user_interests:fruit AND time_stamp:[NOW-(last_visit)DAY TO NOW]" v=$qq} with qq=docID:* and rows=10</td></tr>	
<tr><td></td><td>	To remove any recommendations that userID:x has visited before, include in q;</td></tr>
<tr><td></td><td>	cf="-({!join from=toDocID to=docID fromIndex=clicks_core}userID:x {!join from=fromDocID to=docID fromIndex=clicks_core}userID:x)"</td></tr>	
<tr><td>12.</td><td> next item in non-repeating discovery query with a simple fallback to avoid blind alleys;</td></tr>
<tr><td></td><td>	q={!ctf only2y=true cts="-sessionID:currentSessionID" v=$qq} with qq=docID:currentDocID^10 categoryID:currentCategoryID and rows=1</td></tr>	
<tr><td>13.</td><td> related material for a given document with a simple fallback query for where there are few direct connections;</td></tr>
<tr><td></td><td>	q={!ctf only2y=true base=clicks v=$qq} OR {!ctf base=clicks v=$qqq} with qq=docID:currentDocID^10 and qqq=categoryID:currentCategoryID</td></tr>
</table>

REQUIREMENTS:
------------
<table>
<tr><td>1.</td><td> document core(s) with unique docIDs</td></tr>
<tr><td>2.</td><td> a clicks core for storing user clicks (see attached schema.xml) with a minimum of following fields;</td></tr>
<tr><td></td><td>	timestamp	- timestamp of user's click</td></tr>
<tr><td></td><td>	fromDocID	- user's previous clicked docID in session (may be a null value where necessary)</td></tr>
<tr><td></td><td>	toDocID		- user's current clicked docID</td></tr>
<tr><td></td><td>	other fields (such as userID, usertype, user_interests, to_posn_in_list, user_query etc) are not required but are of course part of the point of using this plugin (see example queries above)</td></tr>
</table>

PARAMETERS IN SOLRCONFIG.XML AND FOR USE IN QUERIES (see attached solrconfig.xml):
--------------------------------------------------------------------------------------
<table>
<tr><td>ctf settings:</td><td></td><td></td></tr>
<tr><td></td><td>	solr_host_url</td><td> 			root containing your data cores</td></tr>
<tr><td></td><td>	document_core_to_query</td><td> 		document core name</td></tr>
<tr><td></td><td>	clicks_core_to_query</td><td>		clicks core name</td></tr>

<tr><td>ctf mappings:</td><td></td><td></td></tr>
<tr><td></td><td>	document_ID_field_name</td><td> 		document core docID field name</td></tr>
<tr><td></td><td>	click_fromID_field_name</td><td> 	clicks core fromDocID field name</td></tr>
<tr><td></td><td>	click_null_fromID_value</td><td> 	clicks core null fromDocID value (for clicks without a fromID)</td></tr>
<tr><td></td><td>	click_toID_field_name</td><td> 		clicks core toDocID field name</td></tr>
<tr><td></td><td>	click_time_stamp_field_name</td><td> 	clicks core timestamp field name</td></tr>

<tr><td>ctf parameters:</td><td></td><td></td></tr>
<tr><td></td><td>	base</td><td>		get primary (1y) items from best query matches or most clicked items (String matches or clicks)</td></tr>
<tr><td></td><td>	restrict</td><td>	show only items with click boosts (String true or false)</td></tr>
<tr><td></td><td>	reorder</td><td>		allow click boosts to effect score and sort (String true or false)</td></tr>
<tr><td></td><td>	extend</td><td> 		include secondary (2y) items (String true or false)</td></tr>
<tr><td></td><td>	only2y</td><td>		show only 2y items (String true or false)</td></tr>
<tr><td></td><td>	cn</td><td> 		number of 1y items to sample (int)</td></tr>
<tr><td></td><td>	cd</td><td> 		average clicks per 1y item (double), controls click sample period</td></tr>
<tr><td></td><td>	cp</td><td>		time integration (int num clicks >0), low values = responsive, high = stable</td></tr>
<tr><td></td><td>	cb</td><td> 		click boost = cb.(fn clicks)^cx (double cb values >0)</td></tr>
<tr><td></td><td>	cx</td><td> 		click boost = cb.(fn clicks)^cx (double cx values >0)</td></tr>
<tr><td></td><td>	ctp</td><td> 		click traffic type for 1y items - a filter query on click traffic (String use ctp="*" for any, ctp="user_type:2", ctp="userID:xxxx", ctp="time_stamp:[NOW-7DAY TO NOW]" or ctp="some function query" etc)</td></tr>
<tr><td></td><td>	cts</td><td>		click traffic type for 2y items - a filter query on click traffic (String as ctp)</td></tr>
<tr><td></td><td>	cs</td><td>	 	proportion of 2y items to allow through, lowest & oldest traffic removed first (double value 0 to 1)</td></tr>
<tr><td></td><td>	cf</td><td>		2y item type - a filter query on 2y items (String use cf="*" for any, cf="cat:2", cf="-doc_type:pdf", cf="published:[NOW-31DAY TO NOW]", cf="some function query" etc)</td></tr>
<tr><td></td><td>	cy</td><td>		position 2y items in list (double value between 0 (next to parent) and 1 (own click boost))</td></tr>
<tr><td></td><td>	cz</td><td>		lookback parameter for observing past query returns at a certain time (Solr date use cz="NOW" or e.g. cz="NOW-7DAY" or cz="2015-07-14T11:32:00Z")</td></tr>
</table>
