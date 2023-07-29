package com.comp4321;

import java.util.List;
import java.util.Vector;
import java.util.Map.Entry;

public class Page {
    public String url;
    public String title;
    public long lastModificationDate;
    public long size;
    public int pageId;
    public Vector<String> parentLinks;
    public Vector<String> childLinks;
    public List<Entry<String, Integer>> frequentWords;
    public double similarityScore;

    public Page (String url, String title, long lastModifiedDate, long size) {
        this.url = url;
        this.title = title;
        this.lastModificationDate = lastModifiedDate;
        this.size = size;
    }

    public Page (String url, String title, long lastModifiedDate, long size, int pageId) {
        this.url = url;
        this.title = title;
        this.lastModificationDate = lastModifiedDate;
        this.size = size;
        this.pageId = pageId;
    }

    public Page (String url) {
        this.url = url;
    }
    
    /**
     * Generate the additional query to be added to the query for getting similar pages
     * @return
     */
    public String getTop5Keywords() {
    	int wordCount = 0;
    	String result = "";
		for (Entry<String, Integer> frequentWord : frequentWords) {
			wordCount++;
			result += frequentWord.getKey() + " ";
			if(wordCount >= 5) break;
		}
		
		return result;
    }
}
