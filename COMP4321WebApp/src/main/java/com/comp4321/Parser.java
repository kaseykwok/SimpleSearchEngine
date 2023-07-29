package com.comp4321;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Vector;

public class Parser {
    private HashSet<String> stopWords;
    private Porter porter;

    public Parser() {
        stopWords = getStopWords();
        porter = new Porter();
    }

    public Vector<String> extractWords(String words, boolean ignoreStopWord, boolean hasToStem) {
        StringTokenizer st = new StringTokenizer(words, " \t\n\r\f,.:;?![]{}()'/\\\"<>+-*#$%^&=`~@|");
        Vector<String> result = new Vector<String>();
        String tempWord;

        while (st.hasMoreTokens()) {
            tempWord = st.nextToken();
            if (!isEmptyWord(tempWord) && !(ignoreStopWord && isStopWord(tempWord))){
                tempWord = tempWord.toLowerCase();
                if (hasToStem) {
                    tempWord = porter.stripAffixes(tempWord);
                    if (isEmptyWord(tempWord)) continue;
                }
                result.add(tempWord);
            } 
        }

        return result;
    }

    public static boolean isEmptyWord(String word){
        return (word == null || word.isEmpty() || word.isBlank() || word == "");
    }

    /**
     * Check whether the word is a stop word
     * @param word
     * @return true if the word is a stop word, otherwise false
     */
    public Boolean isStopWord(String word) {
        return stopWords.contains(word);
    }

    public static HashSet<String> getStopWords() {
        HashSet<String> stopWords = new HashSet<String>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader("stopwords.txt"));
    
            String line;
            while ((line = reader.readLine()) != null) {
                stopWords.add(line);
            }
    
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stopWords;
    }
}
