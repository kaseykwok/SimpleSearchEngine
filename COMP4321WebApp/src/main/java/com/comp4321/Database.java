package com.comp4321;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Database {
    private Connection connection;
    private Statement statement;

    public Database() throws SQLException, IOException, ClassNotFoundException {
        Utility.createFile("comp4321.db");
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:comp4321.db");
        statement = connection.createStatement();
        statement.setQueryTimeout(30);

        initDatabase();
    }

    /**
     * Drop all the tables in the database for manual cleaning
     * @throws SQLException
     */
    private void dropAllTables() throws SQLException {
        statement.executeUpdate("DROP TABLE IF EXISTS page");
        statement.executeUpdate("DROP TABLE IF EXISTS page_property");
        statement.executeUpdate("DROP TABLE IF EXISTS parent_child");
        statement.executeUpdate("DROP TABLE IF EXISTS keyword");
        statement.executeUpdate("DROP TABLE IF EXISTS title_inverted_index");
        statement.executeUpdate("DROP TABLE IF EXISTS body_inverted_index");
        statement.executeUpdate("DROP TABLE IF EXISTS title_bigram");
        statement.executeUpdate("DROP TABLE IF EXISTS body_bigram");
        statement.executeUpdate("DROP TABLE IF EXISTS title_trigram");
        statement.executeUpdate("DROP TABLE IF EXISTS body_trigram");
    }
 
    /**
     * Initialize the database with the necessary tables
     * @throws SQLException
     */
    private void initDatabase() throws SQLException { 
        // dropAllTables();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS page (page_id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT NOT NULL UNIQUE)");
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS page_property (page_id INTEGER PRIMARY KEY, title TEXT, last_modification_date INTEGER NOT NULL, " +
            "size INTEGER NOT NULL, FOREIGN KEY (page_id) REFERENCES page(page_id))");
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS parent_child (parent_page_id INTEGER, child_page_id INTEGER, " + 
            "PRIMARY KEY (parent_page_id, child_page_id), " +
            "FOREIGN KEY (parent_page_id) REFERENCES page(page_id), FOREIGN KEY (child_page_id) REFERENCES page(page_id))"
        );
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS keyword (word_id INTEGER PRIMARY KEY AUTOINCREMENT, word TEXT NOT NULL UNIQUE)");
        // Inverted Index tables
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS title_inverted_index (word_id INTEGER, page_id INTEGER, term_frequency INTEGER, term_weight REAL, " + 
            "PRIMARY KEY (word_id, page_id), " + 
            "FOREIGN KEY (word_id) REFERENCES keyword(word_id), FOREIGN KEY (page_id) REFERENCES page(page_id))"
        );
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS body_inverted_index (word_id INTEGER, page_id INTEGER, term_frequency INTEGER, term_weight REAL, " + 
            "PRIMARY KEY (word_id, page_id), " + 
            "FOREIGN KEY (word_id) REFERENCES keyword(word_id), FOREIGN KEY (page_id) REFERENCES page(page_id))"
        );
        // Bigram tables
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS title_bigram (first_word_id INTEGER, second_word_id INTEGER, page_id INTEGER, term_frequency INTEGER, term_weight REAL, " + 
            "PRIMARY KEY (first_word_id, second_word_id, page_id), " + 
            "FOREIGN KEY (first_word_id) REFERENCES keyword(word_id), FOREIGN KEY (second_word_id) REFERENCES keyword(word_id), FOREIGN KEY (page_id) REFERENCES page(page_id))"
        );
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS body_bigram (first_word_id INTEGER, second_word_id INTEGER, page_id INTEGER, term_frequency INTEGER, term_weight REAL, " + 
            "PRIMARY KEY (first_word_id, second_word_id, page_id), " + 
            "FOREIGN KEY (first_word_id) REFERENCES keyword(word_id), FOREIGN KEY (second_word_id) REFERENCES keyword(word_id), FOREIGN KEY (page_id) REFERENCES page(page_id))"
        );

        // Trigram tables
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS title_trigram (first_word_id INTEGER, second_word_id INTEGER, third_word_id INTEGER, page_id INTEGER, term_frequency INTEGER, term_weight REAL, " + 
            "PRIMARY KEY (first_word_id, second_word_id, third_word_id, page_id), " + 
            "FOREIGN KEY (first_word_id) REFERENCES keyword(word_id), FOREIGN KEY (second_word_id) REFERENCES keyword(word_id), " + 
            "FOREIGN KEY (third_word_id) REFERENCES keyword(word_id), FOREIGN KEY (page_id) REFERENCES page(page_id))"
        );
        statement.executeUpdate(
            "CREATE TABLE IF NOT EXISTS body_trigram (first_word_id INTEGER, second_word_id INTEGER, third_word_id INTEGER, page_id INTEGER, term_frequency INTEGER, term_weight REAL, " + 
            "PRIMARY KEY (first_word_id, second_word_id, third_word_id, page_id), " + 
            "FOREIGN KEY (first_word_id) REFERENCES keyword(word_id), FOREIGN KEY (second_word_id) REFERENCES keyword(word_id), " + 
            "FOREIGN KEY (third_word_id) REFERENCES keyword(word_id), FOREIGN KEY (page_id) REFERENCES page(page_id))"
        );
    }

    /**
     * Disconnect the database once the process ends.
     */
    public void disconnectDatabase() {
        try {
            if(connection != null)
                connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get page ID from database that is mapped to the URL
     * @param url
     * @return page ID
     */
    public int getPageId(String url) {
        try {
            ResultSet rs = statement.executeQuery("SELECT page_id FROM page WHERE url = " + "'" + url + "'");

            if (rs.next()) {
                return rs.getInt("page_id");
            } else {
                return -1;
            }
        } catch (SQLException e) {
            System.err.println("Fail to get page id query");
            return -1;
        }

    }

    /**
     * Retrieve the page properties in the database from the given URL
     * @param url
     * @return a `Page` object that stores all the page properties
     * @throws SQLException
     */
    public Page getPage(String url) throws SQLException {
        ResultSet rs = statement.executeQuery(
            "SELECT page.page_id, url, title, last_modification_date, size FROM page" + 
            " JOIN page_property ON page.page_id = page_property.page_id" + 
            " WHERE url = '" + url + "'"
        );

        if (rs.next()) {
            return new Page(rs.getString("url"), rs.getString("title"), rs.getLong("last_modification_date"), rs.getLong("size"), rs.getInt("page_id"));
        } else {
            return null;
        }
    }

    public Page getPage(int page_id) throws SQLException {
        ResultSet rs = statement.executeQuery(
            "SELECT url, title, last_modification_date, size FROM page" + 
            " JOIN page_property ON page.page_id = page_property.page_id" + 
            " WHERE page.page_id = '" + page_id + "'"
        );

        if (rs.next()) {
            return new Page(rs.getString("url"), rs.getString("title"), rs.getLong("last_modification_date"), rs.getLong("size"), page_id);
        } else {
            return null;
        }
    }

    /**
     * Insert page record to "page" and "page_property" table if they have to be inserted or updated.
     * @param page
     * @return page ID
     * @throws SQLException
     */
    public int insertPageRecord(Page page) throws SQLException {
        int pageId = getPageId(page.url);

        if (pageId == -1) {
            String insertUrlCommand = "INSERT INTO page (url) VALUES ('" + page.url + "')";
            statement.executeUpdate(insertUrlCommand);
            pageId = getPageId(page.url); 
        }

        // Escape single quote in case there is 's in the title, indicate no title if there is none
        String titleSQL = (page.title == null || page.title.isEmpty() || page.title.isBlank()) ? "(No title)" : page.title.replace("'", "''"); 

        String insertCommand =
            "INSERT OR REPLACE INTO page_property (page_id, title, last_modification_date, size) VALUES (" + pageId + ", '" +
            titleSQL + "', '" + page.lastModificationDate + "', " + page.size + ")";

        statement.executeUpdate(insertCommand);

        // If the page has to be reindexed, then remove all the records other related tables in case some relations are deleted later.
        String deleteCommand = "DELETE FROM title_inverted_index WHERE page_id = " + pageId + ";" +
            "DELETE FROM body_inverted_index WHERE page_id = " + pageId + ";" +
            "DELETE FROM title_bigram WHERE page_id = " + pageId + ";" +
            "DELETE FROM body_bigram WHERE page_id = " + pageId + ";" +
            "DELETE FROM title_trigram WHERE page_id = " + pageId + ";" +
            "DELETE FROM body_trigram WHERE page_id = " + pageId + ";" +
            "DELETE FROM parent_child WHERE parent_page_id = " + pageId;
        statement.executeUpdate(deleteCommand);
        
        return pageId;
    }

    /**
     * Insert the parent-child relationship to the "parent_child" table
     * @param parentPageId
     * @param childPageId
     * @throws SQLException
     */
    public void insertParentChild(int parentPageId, int childPageId) throws SQLException {
        // Only insert into parent_child table if the pair was not inside. (One parent page may have repeated child links)
        ResultSet rs = statement.executeQuery("SELECT * FROM parent_child WHERE parent_page_id = " + parentPageId + " AND child_page_id = " + childPageId);

        if (!rs.next()) {
            String insertCommand = "INSERT INTO parent_child (parent_page_id, child_page_id) VALUES (" + parentPageId + ", " + childPageId + ")";
            statement.executeUpdate(insertCommand);
        }
    }

    /**
     * Retrieve the word ID from the given word in the database.
     * @param word
     * @return word ID
     */
    public int getWordId(String word) {
        try {
            String sqlWord = word.replaceAll("'", "''");

            ResultSet rs = statement.executeQuery("SELECT word_id FROM keyword WHERE word = '" + sqlWord +"'");

            if (rs.next()) {
                return rs.getInt("word_id");
            } else {
                return -1;
            }
        } catch (SQLException e) {
            System.err.println("Fail to get word id query: " + word);
            return -1;
        }
    }

    /**
     * Insert the given word into the table "word" if it was not there before.
     * @param word
     * @return word ID
     * @throws SQLException
     */
    public int insertWord(String word) throws SQLException {
        int wordId = getWordId(word);

        if (wordId == -1) {
            String sqlWord = word.replaceAll("'", "''");
            String insertCommand = "INSERT INTO keyword (word) VALUES ('" + sqlWord + "')";
            statement.executeUpdate(insertCommand);
        } 

        return getWordId(word);
    }

    /**
     * Check whether the inverted index record has already existed
     * @param pageId
     * @param wordId
     * @param property Either "title" or "body"
     * @return true if exists, otherwise false.
     */
    public Boolean hasInvertedIndex(int pageId, int wordId, String property) {
        try {
            ResultSet rs = statement.executeQuery("SELECT * FROM " + property + "_inverted_index WHERE page_id = " + pageId + " AND word_id = " + wordId);

            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Fail to check inverted index query");
            return false;
        }
    } 

    /**
     * Insert all index records of the same page to inverted index
     * @param pageId
     * @param wordCounts
     * @param property Either "title" or "body"
     */
    public void insertInvertedIndex(int pageId, HashMap<String, Integer> wordCounts, String property) {
        int wordId;
        String command;

        try {
            for (Map.Entry entry : wordCounts.entrySet()) {
                wordId = insertWord(entry.getKey().toString());
                
                command = "INSERT OR REPLACE INTO " + property + "_inverted_index (page_id, word_id, term_frequency) VALUES (" + pageId + ", " + wordId + ", " + entry.getValue() + ")";

                statement.executeUpdate(command);
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
    } 

    public void insertBigram(int pageId, HashMap<String, Integer> bigramCounts, String property) {
        int firstWordId, secondWordId;
        String command;

        try {
            for (Map.Entry entry : bigramCounts.entrySet()) {
                String[] bigram = entry.getKey().toString().split(",");
                firstWordId = insertWord(bigram[0]);
                secondWordId = insertWord(bigram[1]);
                
                command = "INSERT OR REPLACE INTO " + property + "_bigram (page_id, first_word_id, second_word_id, term_frequency) VALUES (" + 
                    pageId + ", " + firstWordId + ", " + secondWordId + ", " + entry.getValue() + ")";

                statement.executeUpdate(command);
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void insertTrigram(int pageId, HashMap<String, Integer> trigramCounts, String property) {
        int firstWordId, secondWordId, thirdWordId;
        String command;

        try {
            for (Map.Entry entry : trigramCounts.entrySet()) {
                String[] trigram = entry.getKey().toString().split(",");
                firstWordId = insertWord(trigram[0]);
                secondWordId = insertWord(trigram[1]);
                thirdWordId = insertWord(trigram[2]);
                
                command = "INSERT OR REPLACE INTO " + property + "_trigram (page_id, first_word_id, second_word_id, third_word_id, term_frequency) VALUES (" + 
                    pageId + ", " + firstWordId + ", " + secondWordId + ", " + thirdWordId + ", " + entry.getValue() + ")";

                statement.executeUpdate(command);
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
    
    public void updateTermWeights() throws SQLException {
        updateInvertedTermWeights("body");
        updateBigramTermWeights("body");
        updateTrigramTermWeights("body");

        updateInvertedTermWeights("title");
        updateBigramTermWeights("title");
        updateTrigramTermWeights("title");
    }

    private void updateInvertedTermWeights(String property) throws SQLException {
        String query = "UPDATE " + property + "_inverted_index as A" +
            " SET term_weight = (0.5 + 0.5 * CAST(term_frequency as REAL) / CAST(max_tf as REAL) ) * log2(1 + CAST(total_pages as REAL) / CAST(document_frequency as REAL))" +
            " FROM (" +
            "    SELECT  page_id, MAX(term_frequency) as max_tf FROM " + property + "_inverted_index as B" +
            "    WHERE B.page_id = page_id" +
            "    GROUP BY B.page_id" +
            ") as C," +
            "( SELECT COUNT(*) AS total_pages FROM page ) as D," +
            "(" +
            "    SELECT COUNT(*) AS document_frequency,  word_id FROM " + property + "_inverted_index" +
            "    GROUP BY word_id" +
            ") as E" +
            " WHERE A.page_id = C.page_id AND A.word_id = E.word_id";

        statement.executeUpdate(query);

        // UPDATE body_inverted_index as A
        // SET term_weight =  (0.5 + 0.5 * term_frequency / max_tf) * log2(1 + total_pages / document_frequency)
        // FROM (
        //     SELECT  page_id, MAX(term_frequency) as max_tf FROM body_inverted_index as B
        //     WHERE B.page_id = page_id
        //     GROUP BY B.page_id
        // ) as C,
        // ( SELECT COUNT(*) AS total_pages FROM page ) as D,
        // (
        //     SELECT COUNT(*) AS document_frequency,  word_id FROM body_inverted_index
        //     GROUP BY word_id
        // ) as E
        // WHERE A.page_id = C.page_id AND A.word_id = E.word_id
    }

    private void updateBigramTermWeights(String property) throws SQLException {
        String query = "UPDATE " + property + "_bigram as A" + 
            " SET term_weight = (0.5 + 0.5 * CAST(term_frequency as REAL) / CAST(max_tf as REAL) ) * log2(1 + CAST(total_pages as REAL) / CAST(document_frequency as REAL))" +
            " FROM (" + 
            "    SELECT  page_id, MAX(term_frequency) as max_tf FROM " + property + "_bigram as B" +
            "    WHERE B.page_id = page_id" +
            "    GROUP BY B.page_id" +
            ") as C," +
            "( SELECT COUNT(*) AS total_pages FROM page ) as D," +
            "(" +
            "    SELECT COUNT(*) AS document_frequency,  first_word_id, second_word_id FROM " + property + "_bigram" +
            "    GROUP BY first_word_id, second_word_id" +
            ") as E" +
            " WHERE A.page_id = C.page_id AND A.first_word_id = E.first_word_id AND A.second_word_id = E.second_word_id ";

        statement.executeUpdate(query);
        
        /*
        UPDATE body_bigram as A
        SET term_weight = (0.5 + 0.5 * CAST(term_frequency as REAL) / CAST(max_tf as REAL) ) * log2(1 + CAST(total_pages as REAL) / CAST(document_frequency as REAL))
        FROM (
            SELECT  page_id, MAX(term_frequency) as max_tf FROM body_bigram as B
            WHERE B.page_id = page_id
            GROUP BY B.page_id
        ) as C,
        ( SELECT COUNT(*) AS total_pages FROM page ) as D,
        (
            SELECT COUNT(*) AS document_frequency,  first_word_id, second_word_id FROM body_bigram
            GROUP BY first_word_id, second_word_id
            ORDER BY document_frequency DESC
        ) as E
        WHERE A.page_id = C.page_id AND A.first_word_id = E.first_word_id AND A.second_word_id = E.second_word_id 
        */
    }

    private void updateTrigramTermWeights(String property) throws SQLException {
        String query = "UPDATE " + property + "_trigram as A" +
            " SET term_weight = (0.5 + 0.5 * CAST(term_frequency as REAL) / CAST(max_tf as REAL) ) * log2(1 + CAST(total_pages as REAL) / CAST(document_frequency as REAL))" +
            " FROM (" +
            "     SELECT  page_id, MAX(term_frequency) as max_tf FROM " + property + "_trigram as B" +
            "     WHERE B.page_id = page_id" +
            "     GROUP BY B.page_id" +
            " ) as C," +
            " ( SELECT COUNT(*) AS total_pages FROM page ) as D," +
            " (" +
            "     SELECT COUNT(*) AS document_frequency,  first_word_id, second_word_id, third_word_id FROM " + property + "_trigram" +
            "     GROUP BY first_word_id, second_word_id, third_word_id" +
            " ) as E" +
            " WHERE A.page_id = C.page_id AND A.first_word_id = E.first_word_id " +
            " AND A.second_word_id = E.second_word_id AND A.third_word_id = E.third_word_id";

        statement.executeUpdate(query);
    }

    // Function for Search Engine
    public HashMap<Integer, Double> getInvertedSimilarityScores(Vector<String> terms, String property) throws SQLException {
		String[] termsArr = terms.toArray(new String[terms.size()]);
		String joinedTerms = Stream.of(termsArr).collect(Collectors.joining("','", "'", "'"));

        String query = " SELECT A.page_id, A.weight_sum / (sqrt(B.weight_sq_sum) * sqrt(" + terms.size() + ")) as cosine_score" +
            " FROM (" +
            "     SELECT page_id, SUM(term_weight) as weight_sum FROM " + property + "_inverted_index as S" +
            "     INNER JOIN keyword as T ON S.word_id = T.word_id" +
            "     WHERE T.word IN (" + joinedTerms + ")" +
            "     GROUP BY page_id" +
            " ) as A, (" +
            "     SELECT page_id, SUM(term_weight*term_weight) as weight_sq_sum FROM " + property + "_inverted_index" +
            "     GROUP BY page_id" +
            " ) as B" +
            " WHERE A.page_id = B.page_id" +
            " ORDER BY cosine_score DESC";

        ResultSet rs = statement.executeQuery(query);

        HashMap<Integer, Double> pageScores = new HashMap<>();

        while (rs.next()) {
            pageScores.put(rs.getInt("page_id"), rs.getDouble("cosine_score"));
        }
        
        return pageScores;

        // SELECT A.page_id, A.weight_sum / (sqrt(B.weight_sq_sum) * sqrt(terms.size())) as cosine_score
        // FROM (
        //     SELECT page_id, SUM(term_weight) as weight_sum FROM body_inverted_index as S
        //     INNER JOIN keyword as T ON S.word_id = T.word_id
        //     WHERE T.word IN ('bbc', 'new')
        //     GROUP BY page_id
        // ) as A, (
        //     SELECT page_id, SUM(term_weight*term_weight) as weight_sq_sum FROM body_inverted_index
        //     GROUP BY page_id
        // ) as B
        // WHERE A.page_id = B.page_id
        // ORDER BY cosine_score DESC
    }

    public HashMap<Integer, Double> getBigramSimilarityScores(Vector<String> bigrams, String property) throws SQLException {
        String[] bigramsArr = bigrams.toArray(new String[bigrams.size()]);
		String joinedTerms = Stream.of(bigramsArr).collect(Collectors.joining("','", "'", "'"));

        String query = "SELECT A.page_id, A.weight_sum / (sqrt(B.weight_sq_sum) * sqrt(" + bigrams.size() + ")) as cosine_score" +
            " FROM (" +
            "     SELECT page_id, SUM(term_weight) as weight_sum FROM " + property + "_bigram as S" +
            "     LEFT JOIN keyword as T ON S.first_word_id = T.word_id" +
            "     LEFT JOIN keyword as U ON S.second_word_id = U.word_id" +
            "     WHERE T.word || ' ' || U.word IN (" + joinedTerms + ")" +
            "     GROUP BY page_id" +
            " ) as A, (" +
            "     SELECT page_id, SUM(term_weight*term_weight) as weight_sq_sum FROM " + property + "_bigram" +
            "     GROUP BY page_id" +
            " ) as B" +
            " WHERE A.page_id = B.page_id";

        ResultSet rs = statement.executeQuery(query);

        HashMap<Integer, Double> pageScores = new HashMap<>();

        while (rs.next()) {
            pageScores.put(rs.getInt("page_id"), rs.getDouble("cosine_score"));
        }

        return pageScores;

        // SELECT A.page_id, A.weight_sum / B.weight_sq_sum as cosine_score
        // FROM (
        //     SELECT page_id, SUM(term_weight) as weight_sum FROM body_bigram as S
        //     LEFT JOIN keyword as T ON S.first_word_id = T.word_id
        //     LEFT JOIN keyword as U ON S.second_word_id = U.word_id
        //     WHERE T.word || ' ' || U.word IN ('test page', 'movi list')
        //     GROUP BY page_id
        // ) as A, (
        //     SELECT page_id, SUM(term_weight*term_weight) as weight_sq_sum FROM body_bigram
        //     GROUP BY page_id
        // ) as B
        // WHERE A.page_id = B.page_id
    }

    public HashMap<Integer, Double> getTrigramSimilarityScores(Vector<String> trigrams, String property) throws SQLException {
        String[] trigramsArr = trigrams.toArray(new String[trigrams.size()]);
		String joinedTerms = Stream.of(trigramsArr).collect(Collectors.joining("','", "'", "'"));

        String query = "SELECT A.page_id, A.weight_sum / (sqrt(B.weight_sq_sum) * sqrt(" + trigrams.size() + ")) as cosine_score" +
            " FROM (" +
            "     SELECT page_id, SUM(term_weight) as weight_sum FROM " + property + "_trigram as S" +
            "     LEFT JOIN keyword as T ON S.first_word_id = T.word_id" +
            "     LEFT JOIN keyword as U ON S.second_word_id = U.word_id" +
            "     LEFT JOIN keyword as V ON S.third_word_id = V.word_id" +
            "     WHERE T.word || ' ' || U.word || ' ' || V.word IN (" + joinedTerms + ")" +
            "     GROUP BY page_id" +
            " ) as A, (" +
            "     SELECT page_id, SUM(term_weight*term_weight) as weight_sq_sum FROM " + property + "_trigram" +
            "     GROUP BY page_id" +
            " ) as B" +
            " WHERE A.page_id = B.page_id";

        ResultSet rs = statement.executeQuery(query);

        HashMap<Integer, Double> pageScores = new HashMap<>();

        while (rs.next()) {
            pageScores.put(rs.getInt("page_id"), rs.getDouble("cosine_score"));
        }

        return pageScores;
    }

    private Vector<String> get10ParentLinks(int pageId) throws SQLException {
        Vector<String> childLinks = new Vector<>();

        ResultSet rs = statement.executeQuery(
            "SELECT page.url FROM page" +
            " INNER JOIN parent_child ON page.page_id = parent_child.parent_page_id" +
            " WHERE parent_child.child_page_id = " + pageId +
            " LIMIT 10"
        );

        while(rs.next()) {
            childLinks.add(rs.getString("url"));
        }

        return childLinks;
    }

    private Vector<String> get10ChildLinks(int pageId) throws SQLException {
        Vector<String> childLinks = new Vector<>();

        ResultSet rs = statement.executeQuery(
            "SELECT page.url FROM page" +
            " INNER JOIN parent_child ON page.page_id = parent_child.child_page_id" +
            " WHERE parent_child.parent_page_id = " + pageId +
            " LIMIT 10"
        );

        while(rs.next()) {
            childLinks.add(rs.getString("url"));
        }

        return childLinks;
    }
    
    private List<Entry<String, Integer>> getAllFrequentWords(int pageId) throws SQLException {
        List<Entry<String, Integer>> frequentWords = new ArrayList<>();

        String query = "SELECT word, SUM(term_frequency) as total_term_frequency " +
            " FROM (" +
            "     SELECT * FROM title_inverted_index as S" +
            "     UNION " +
            "     SELECT * FROM body_inverted_index as T " +
            " ) as U" +
            " INNER JOIN keyword ON keyword.word_id = U.word_id" +
            " WHERE page_id = " + pageId +
            " GROUP BY U.word_id" +
            " ORDER BY total_term_frequency DESC";
        
        ResultSet rs = statement.executeQuery(query);

        while (rs.next()) {
            frequentWords.add(Map.entry(rs.getString("word"), rs.getInt("total_term_frequency")));
        }

        return frequentWords;
    }

    public Vector<Page> getResultPages(List<Entry<Integer, Double>> documentScoreList) throws SQLException {
        Vector<Page> resultPages = new Vector<>();
        Page currentPage;
        int pageId;

        for (Entry<Integer,Double> documentScore : documentScoreList) {
            pageId = documentScore.getKey();
            currentPage = getPage(pageId);

            currentPage.similarityScore = documentScore.getValue();
            currentPage.childLinks = get10ChildLinks(pageId);
            currentPage.parentLinks = get10ParentLinks(pageId);
            currentPage.frequentWords = getAllFrequentWords(pageId);

            resultPages.add(currentPage);

            if (resultPages.size() == 50) break; // Show maximum 50 pages 
        }

        return resultPages;
    }
}
