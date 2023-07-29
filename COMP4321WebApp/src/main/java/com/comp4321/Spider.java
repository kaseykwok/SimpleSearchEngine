package com.comp4321;

import java.net.URL;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map;
import org.htmlparser.beans.LinkBean;
import org.htmlparser.beans.StringBean;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import javafx.util.Pair;

import org.jsoup.Connection;

public class Spider {
    private Database db;
    private Indexer indexer;
    private Parser parser;

    public Spider() throws SQLException, IOException, ClassNotFoundException {
        this.db = new Database();
        this.indexer = new Indexer();
        this.parser = new Parser();
    }

    /**
     * Get header fields through the connection of HttpURLConnection.
     * @param url the URL of the page to retrieve
     * @param countContentLength true if no "Content-Length" field can be found from the response header
     * @return header fields map that might store "Last-Modified" or "Content-Length" key-value pair
     */
    private Map<String, Long> getHeaderFields(String url, Boolean countContentLength) {
        Map<String, Long> headerFields = new HashMap<>();

        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.connect();  
            headerFields.put("Last-Modified", (connection.getLastModified() == 0) ? connection.getDate() : connection.getLastModified());

            if (countContentLength) {
                long length = 0;

                BufferedReader in = new BufferedReader(
                new InputStreamReader(urlObj.openStream()));
                
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    length += inputLine.length();
                }
                in.close();

                headerFields.put("Content-Length", length);
            }

            connection.disconnect();
        } catch (Exception e){
            System.err.println("Fail to get header fields: " + e.toString());
        } 

        return headerFields;
    }
    
    /**
     * Extract all the child links from the parent page's URL
     * @param parentURL
     * @return an array of child links
     */
    private URL[] extractChildLinks(String parentURL)
	{
        try {
            LinkBean lb = new LinkBean();
            lb.setURL(parentURL);
            URL[] URL_array = lb.getLinks();
            return URL_array;
        } catch (Exception e) {
            System.err.println("Fail to extract child links: " + e.toString());
            return new URL[] {};
        }
	}

    /**
     * Extract all the words from the page's body
     * @param url
     * @return vector of all the words in the body
     */
    private Vector<String> extractBodyWords(String url, Vector<String> titleWords) {
        try {
            StringBean sb = new StringBean();
            sb.setURL(url);

            Vector<String> bodyWords = parser.extractWords(sb.getStrings(), false, false);
            // getStrings() get both the title and body
            for (int i=titleWords.size() - 1; i>=0; i--){
                bodyWords.remove(i);
            }

            return bodyWords;
        } catch (Exception e) {
            System.err.println("Fail to extract words: " + e.toString());
            return null;
        }
    }

    /**
     * Get all page properties and return a `Page` object.
     * @param url
     * @return page that stores all necessary information for page properties
     */
    private Page getPageProperties(String url) {
        Connection connection; 
        Document doc;
        try {
            connection = Jsoup.connect(url);
            doc = connection.get();
        } catch (Exception e) {
            System.out.println("\tIgnored url : "+ url);
            return null;
        }

        String title = doc.title();

        String contentLength = connection.response().header("Content-Length");
        
        long size;
        Map<String, Long> headerFields = getHeaderFields(url, (contentLength == null));

        if (contentLength == null) {
            size = headerFields.get("Content-Length");
        } else {
            size = Long.valueOf(contentLength);
        }

        return new Page(url, title, headerFields.get("Last-Modified"), size);
    }

    /**
     * Crawl the pages through BFS strategy and save the results in database.
     * @param parentUrl
     */
    public void crawlPages(String parentUrl) {
        Vector<String> visitedUrls = new Vector<>();
        URLQueue urlQueue = new URLQueue(); // Queue for reindexing/revisiting
        int pageCount = 0;

        try {
            Page rootPage = getPageProperties(parentUrl);
            if (rootPage != null) { // Can be connected and retrieved
                visitedUrls.add(parentUrl);
    
                Page oldRootPage = db.getPage(parentUrl);
                // If old root page does not exist, or if it exists but has a later last modification date, then it has to be retrieved.
                if (oldRootPage == null || (oldRootPage != null && oldRootPage.lastModificationDate < rootPage.lastModificationDate)) {
                    db.insertPageRecord(rootPage);
                    urlQueue.add(parentUrl, true);
                } else { // old page that exists but does not have to be reindexed. But still need to check the child pages.
                    urlQueue.add(parentUrl, false);
                }
            }

            while (urlQueue.size() != 0) {
                Pair<String, Boolean> urlRetrieve = urlQueue.poll();
                String url = urlRetrieve.getKey();
                Boolean hasToIndex = urlRetrieve.getValue();

                Page parentPage = db.getPage(url);

                pageCount++;
                System.out.println(pageCount + ":" + url);

                // Only index pages that have to be upated/inserted. There are links that are in the queue only for checking the child pages' update.
                if (hasToIndex) {
                    System.out.println("\tReindexing...");
                    // Extract the words
                    Vector<String> titleWords = parser.extractWords(parentPage.title, false, false);
                    Vector<String> words = extractBodyWords(url, titleWords);
    
                    // Index the words into inverted index, bigram and trigram respectively
                    indexer.indexPage(parentPage.pageId, titleWords, db, "title");
                    indexer.indexPage(parentPage.pageId, words, db, "body");
                }
                
                Vector<URL> visitedChildUrls = new Vector<>();
                // URL[] childUrls = {}; // Test root page only
                URL[] childUrls = extractChildLinks(url);
                int childPageId;

                for (URL childUrl : childUrls) {
                    // Skip the child url if it has already been visited in the for loop as one parent might have multiple children with same URL.
                    if (visitedChildUrls.contains(childUrl)) {
                        System.out.println("\tRepeated child: " + childUrl);
                        continue;
                    } else {
                        visitedChildUrls.add(childUrl);
                    }

                    String childUrlStr = childUrl.toString();

                    if (!visitedUrls.contains(childUrlStr)) {
                        Page childPage = getPageProperties(childUrlStr);
                        if (childPage == null) continue; // Cannot agree in SSL Handshake
                        
                        visitedUrls.add(childUrlStr);

                        Page oldChildPage = db.getPage(childUrlStr);
                        
                        // If old page does not exist, or if it exists but has a later last modification date, then it has to be retrieved.
                        if (oldChildPage == null || (oldChildPage != null && oldChildPage.lastModificationDate < childPage.lastModificationDate)) {
                            childPageId = db.insertPageRecord(childPage);
                            urlQueue.add(childUrlStr, true);
                            System.out.println("\tNew/Reindex: " + childUrlStr);
                        } else { // If it exists and does not need to be reindexed.
                            childPageId = db.getPageId(childUrlStr);
                            urlQueue.add(childUrlStr, false);
                            System.out.println("\tOld (no update): " + childUrlStr);
                        }
                    } else { // Pages that have been visited but is also a child
                        childPageId = db.getPageId(childUrlStr);
                        System.out.println("\tOld (visited): " + childUrlStr);
                    }

                    // Insert parent-child relationship to database regardless of whether it has to be retrieved again.
                    db.insertParentChild(parentPage.pageId, childPageId);
                }

                System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Spider spider = new Spider();
    
            Instant start = Instant.now();
            System.out.println("Spider is crawling...");
            spider.crawlPages("https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm");
            // spider.crawlPages("https://www.cse.ust.hk/~kwtleung/COMP4321/Movie/18.html");
            // spider.crawlPages("https://www.cse.ust.hk/");
            Instant end = Instant.now();
            System.out.println("Spider has finished crawling.");

            System.out.println("Updating term weights...");
            spider.db.updateTermWeights();
            System.out.println("Term weights updated.");

            long timeElapsed = Duration.between(start, end).toSeconds();
            System.out.println("Time elapsed: " + (int)(timeElapsed / 60) + "m " + (timeElapsed % 60) + "s");

            spider.db.disconnectDatabase();
        } catch (SQLException e) {
            System.err.println("Fail to connect database");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Fail to create comp4321.db file");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
    }
}
