package com.comp4321;

import java.util.Vector;
import java.util.HashMap;

public class Indexer {
    private Parser parser;
    private Porter porter;

    public Indexer() {
        // Read all the stop word from the file
        parser = new Parser();
        porter = new Porter();
    }

    private HashMap<String, Integer> generateBigrams(Vector<String> words) {
        HashMap<String, Integer> bigramCounts = new HashMap<>();
        for(int i=0; i<words.size() - 1; i++) {
            // Stop word filtering
            if (parser.isStopWord(words.get(i)) || parser.isStopWord(words.get(i+1))) continue;

            String newBigram = words.get(i) + "," + words.get(i+1);
            if (bigramCounts.containsKey(newBigram)) {
                bigramCounts.replace(newBigram, bigramCounts.get(newBigram) + 1);
            } else {
                bigramCounts.put(newBigram, 1);
            }
        }

        return bigramCounts;
    }

    private HashMap<String, Integer> generateTrigrams(Vector<String> words) {
        HashMap<String, Integer> trigramCounts = new HashMap<>();
        for(int i=0; i<words.size() - 2; i++) {
            // Stop word filtering
            if (parser.isStopWord(words.get(i)) || parser.isStopWord(words.get(i+1)) || parser.isStopWord(words.get(i+2))) continue;

            String newTrigram = words.get(i) + "," + words.get(i+1) + "," + words.get(i+2);
            if (trigramCounts.containsKey(newTrigram)) {
                trigramCounts.replace(newTrigram, trigramCounts.get(newTrigram) + 1);
            } else {
                trigramCounts.put(newTrigram, 1);
            }
        }

        return trigramCounts;
    }

    /**
     * Process the keywords and count the frequency of each of them, and store the result to the database
     * @param pageId
     * @param words
     * @param db
     */
    public void indexPage(int pageId, Vector<String> words, Database db, String property) {
        HashMap<String, Integer> wordCounts = new HashMap<>();
        Vector<String> ngramWords = new Vector<>();
        String stemmedWord;

        for (String word : words) {
            if (Parser.isEmptyWord(word)) continue; // Ignore if it is a stop word;

            if (parser.isStopWord(word)){
                ngramWords.add(word);
                continue;
            }

            stemmedWord = porter.stripAffixes(word);

            if (Parser.isEmptyWord(stemmedWord)) continue; // Ignore if the stemmed word becomes blank.

            ngramWords.add(stemmedWord);

            if (wordCounts.containsKey(stemmedWord)) {
                wordCounts.replace(stemmedWord, wordCounts.get(stemmedWord) + 1);
            } else {
                wordCounts.put(stemmedWord, 1);
            }
        }

        HashMap<String, Integer> bigramCounts = generateBigrams(ngramWords);
        HashMap<String, Integer> trigramCounts = generateTrigrams(ngramWords);

        db.insertInvertedIndex(pageId, wordCounts, property);
        db.insertBigram(pageId, bigramCounts, property);
        db.insertTrigram(pageId, trigramCounts, property);
    }
}
