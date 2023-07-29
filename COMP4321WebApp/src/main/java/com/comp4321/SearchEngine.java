package com.comp4321;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchEngine {
    private Database db;
	private Parser parser;

    public SearchEngine() throws SQLException, IOException, ClassNotFoundException {
        this.db = new Database();
		this.parser = new Parser();
    }

	public Vector<Page> getQueryResult(String query) throws SQLException {
		List<Entry<Integer, Double>> documentScoreList = parseQuery(query);

		Vector<Page> resultPages = db.getResultPages(documentScoreList);

		return resultPages;
	}

	private void displayResultPages(Vector<Page> resultPages) {
		if (resultPages.size() == 0) {
			System.out.println("\tNo pages found that matches the query.");
			return;
		}

		for (Page resultPage : resultPages) {
			System.out.println(String.format("%.6f", resultPage.similarityScore) + "\t" + resultPage.title);
			System.out.println("\t\t" + resultPage.url);
			System.out.println("\t\t" + Utility.convertDateToString(resultPage.lastModificationDate) + ", " + resultPage.size);
			System.out.print("\t\t");

			int wordCount = 0;
			for (Entry<String, Integer> frequentWord : resultPage.frequentWords) {
				wordCount++;
				System.out.print(frequentWord.getKey() + " " + frequentWord.getValue() + "; ");
				
				if(wordCount >= 5) break;
			}
			System.out.println();

			System.out.println("\t\tParent Links: ");
			for (String parentLink : resultPage.parentLinks) {
				System.out.println("\t\t\t" + parentLink);
			}

			System.out.println("\t\tChild Links: ");
			for (String childLink : resultPage.childLinks) {
				System.out.println("\t\t\t" + childLink);
			}

			System.out.println();
		}
	}

	private List<Entry<Integer, Double>> parseQuery(String query) throws SQLException {
		Vector<String> phrases = new Vector<>(); 	// phrases in double quotation
		Vector<String> terms;  	// terms that are not in double quotation

		Pattern pattern = Pattern.compile("\"([^\"]*)\"");
		Matcher matcher = pattern.matcher(query);
		while (matcher.find()) {
			phrases.add(matcher.group(1).strip());
			query = query.replace("\"" + matcher.group(1) + "\"", " ");
		}

		terms = parser.extractWords(query, true, true); // Extract words and ignore the stop words, stem each word

		Vector<String> tempPhrase;
		Vector<String> bigramPhrases = new Vector<>();
		Vector<String> trigramPhrases = new Vector<>();

		for (String phrase : phrases) {
			tempPhrase = parser.extractWords(phrase, false, true); // No stop word in phrase, stem each word

			if (tempPhrase.size() == 1) {
				terms.add(tempPhrase.get(0));
			} else if (tempPhrase.size() == 2) {
				bigramPhrases.add(tempPhrase.get(0) + " " + tempPhrase.get(1));
			} else if (tempPhrase.size() == 3) {
				trigramPhrases.add(tempPhrase.get(0) + " " + tempPhrase.get(1) + " " + tempPhrase.get(2));
			}
		}

		return getDocumentSimilarityScores(terms, bigramPhrases, trigramPhrases);
	}

	/**
	 * Compute the final similarity scores after weighting different scores
	 * @param terms
	 * @param bigrams
	 * @param trigrams
	 * @return
	 * @throws SQLException
	 */
	private List<Entry<Integer, Double>> getDocumentSimilarityScores(Vector<String> terms, Vector<String> bigrams, Vector<String> trigrams) throws SQLException {
		HashMap<Integer, Double> documentScores = db.getInvertedSimilarityScores(terms, "body");

		HashMap<Integer, Double> tempDocumentScores;

		// Add body bigram similarity score. Has a weight of 1.1. 
		tempDocumentScores = db.getBigramSimilarityScores(bigrams, "body");
		for (Entry<Integer, Double> entry : tempDocumentScores.entrySet()) {
			if (documentScores.containsKey(entry.getKey())) {
				documentScores.replace((int)entry.getKey(), 1.1 * (double)entry.getValue() + documentScores.get(entry.getKey()));
			} else {
				documentScores.put((int)entry.getKey(), (double)entry.getValue());
			}
		}

		// Add body trigram similarity score. Has a weight of 1.2.
		tempDocumentScores = db.getTrigramSimilarityScores(trigrams, "body");
		for (Entry<Integer, Double> entry : tempDocumentScores.entrySet()) {
			if (documentScores.containsKey(entry.getKey())) {
				documentScores.replace((int)entry.getKey(), 1.2 * (double)entry.getValue() + documentScores.get(entry.getKey()));
			} else {
				documentScores.put((int)entry.getKey(), (double)entry.getValue());
			}
		}

		// Add title inverted index score. Weight = 1.8
		tempDocumentScores = db.getInvertedSimilarityScores(terms, "title");
		for (Entry<Integer, Double> entry : tempDocumentScores.entrySet()) {
			if (documentScores.containsKey(entry.getKey())) {
				documentScores.replace((int)entry.getKey(), 1.8 * (double)entry.getValue() + documentScores.get(entry.getKey()));
			} else {
				documentScores.put((int)entry.getKey(), (double)entry.getValue());
			}
		}

		// Add title bigram scores. Weight = 1.9
		tempDocumentScores = db.getBigramSimilarityScores(bigrams, "title");
		for (Entry<Integer, Double> entry : tempDocumentScores.entrySet()) {
			if (documentScores.containsKey(entry.getKey())) {
				documentScores.replace((int)entry.getKey(), 1.9 * (double)entry.getValue() + documentScores.get(entry.getKey()));
			} else {
				documentScores.put((int)entry.getKey(), (double)entry.getValue());
			}
		}

		// Add title trigram scores. Weight  = 2.0
		tempDocumentScores = db.getTrigramSimilarityScores(trigrams, "title");
		for (Entry<Integer, Double> entry : tempDocumentScores.entrySet()) {
			if (documentScores.containsKey(entry.getKey())) {
				documentScores.replace(entry.getKey(), 2.0 * entry.getValue() + documentScores.get(entry.getKey()));
			} else {
				documentScores.put(entry.getKey(), entry.getValue());
			}
		}

		List<Entry<Integer, Double>> documentScoreList = new ArrayList<>(documentScores.entrySet());
		if (documentScoreList.size() == 0) {return documentScoreList;}

		documentScoreList.sort(Entry.<Integer, Double>comparingByValue().reversed());
		double maxScore = documentScoreList.get(0).getValue();

		// Normalize the scores to [0-100]
		for(int i=0; i<documentScoreList.size(); i++) {
			documentScoreList.set(i, Map.entry(documentScoreList.get(i).getKey(), documentScoreList.get(i).getValue() / maxScore * 100.0));
			// System.out.println(documentScoreList.get(i).getKey() + ": " + documentScoreList.get(i).getValue());
		}

		return documentScoreList;
	}

    public static void main(String[] args) {
        try {
			SearchEngine se = new SearchEngine();

			String input="";
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

			do {
				System.out.print("Please input your query (Press Enter to close the search engine): ");
				input = in.readLine();

				if (input.length() > 0) {
					Vector<Page> queryResult = se.getQueryResult(input);
					se.displayResultPages(queryResult);
				}

				System.out.println();
			} while(input.length() > 0);

			se.db.disconnectDatabase();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
    }
}

/*
 * 3. A retrieval function (or called the search engine) that compares a list of query terms against the inverted file and returns the top documents, 
 *    up to a maximum of 50, to the user in a ranked order according to the vector space model. As noted about, phrase must be supported, e.g., “hong kong” universities
 *      * Term weighting formula is based on tfxidf/max(tf).
 *      * Document similarity is based on cosine similarity measure. (query weight all = 1)
 *      * Derive and implement a mechanism to favor matches in title. For example, a match in the title would significantly boost the rank of a page
 */
