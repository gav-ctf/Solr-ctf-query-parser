# Solr-ctf-query-parser
Ranks and expands Solr query returns using clickstream data

SOME EXAMPLE CTF QUERIES (parameters are described below):
---------------------------------------------------------

1. search in text field of your documents core for "melon" at your default CTF settings;
	q={!ctf}text:melon
	or
	q={!ctf v=$qq} with qq=text:melon

2. weighted search for "melon" in title or body fields using only "botanist" click traffic but otherwise default settings;
	q={!ctf ctp="user:botanist" cts="user:botanist" v=$qq} with qq=title:melon^2 body:melon

3. weighted search for "melon" or "cherry" blending click traffic & publication_date boosting;
	q={!boost b=recip(ms(NOW/HOUR,publication_date),3.16e-11,1,1)}{!ctf cb=5 cx=2 v=$qq} with qq=text:melon^3 text:cherry^2
	or
	q={!ctf cb=5 cx=2 v=$qq} with qq=({!boost b=recip(ms(NOW/HOUR,publication_date),3.16e-11,1,1)}body:melon^3 {!boost b=recip(ms(NOW/HOUR,publication_date),3.16e-11,1,1)}body:cherry^2) etc

4. filtered search for "orange" with all returned items in category fruit;
	q={!ctf v=$qq} with qq=text:orange and fq=category:fruit

5. filtered search for "orange" with just secondary items in category fruit;
	q={!ctf cf="category:fruit" v=$qq} with qq=text:orange

6. filtered search for "orange" with just primary items in category fruit;
	q={!ctf v=$qq} with qq=+text:orange +category:fruit

7. search for "lychee" with a high sensitivity to changes over time, recoiling quickly to the unmodified search return;
	q={!ctf cp=5 cd=2 ctp="time_stamp:[NOW-1DAY TO NOW]" v=$qq} with qq=text:lychee

8. search for "lychee" in title field preserving original sort & including best 10% of secondary items with title word like "lychee";
	q={!ctf reorder=false cf="title:lychee~0.5" cs=0.1 v=$qq} with qq=title:lychee

9. last week's search for "kiwi" based on the 10 most clicked "kiwi" items, with the best 20% of non-pdf secondary items that are then pushed down the list to help maximise improvement;
	q={!ctf base=clicks cn=10 cz="NOW-7DAY" cs=0.2 cf="-doctype:pdf" cy=0.95 v=$qq} with qq=text:kiwi

10. top 5 most visited "lemon" recipes by user types 2 & 3 over the last 7 days;
	q={!ctf cn=5 base=clicks restrict=true extend=false ctp="+user_type:(2 3) +time_stamp:[NOW-7DAY/DAY TO NOW]" v=$qq} with qq=recipe:*lemon*

11. top 10 recommendations for userID:x, based on userID:x's up to 20 most visited items over the last month and click traffic through those items by any other user with an interest in "fruit" since userID:x's last visit;
	q={!ctf base=clicks only2y=true cn=20 ctp="userID:x AND time_stamp:[NOW-31DAY TO NOW]" cts="-userID:x AND user_interests:fruit AND time_stamp:[NOW-(last_visit)DAY TO NOW]" v=$qq} with qq=docID:* and rows=10
	
	To remove any recommendations that userID:x has visited before, include in q;
	cf="-({!join from=toDocID to=docID fromIndex=clicks_core}userID:x {!join from=fromDocID to=docID fromIndex=clicks_core}userID:x)"
	
12. next item in non-repeating discovery query with a simple fallback to avoid blind alleys;
	q={!ctf only2y=true cts="-sessionID:currentSessionID" v=$qq} with qq=docID:currentDocID^10 categoryID:currentCategoryID and rows=1
	
13. related material for a given document with a simple fallback query for where there are few direct connections;
	q={!ctf only2y=true base=clicks v=$qq} OR {!ctf base=clicks v=$qqq} with qq=docID:currentDocID^10 and qqq=categoryID:currentCategoryID
