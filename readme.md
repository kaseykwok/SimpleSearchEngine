# COMP4321 Project Phase 2

Name: Kwok Ying Kwan Kasey

## Installation instructions

### Download the libraries
1. Download Apache Tomcat 10.1.8 (`apache-tomcat-10.1.8`) from https://tomcat.apache.org/download-10.cgi and store it at the the root directory
2. Create the directory `/COMP4321WebApp/src/main/webapp/WEB-INF/` and add a `lib` and a `classes` directory into the `WEB-INF` directory.
    - Download `sqlite-jdbc-3.41.0.0.jar` from https://github.com/xerial/sqlite-jdbc/releases/tag/3.41.0.0 and store it in `lib`
    - Download `jsoup-1.15.4.jar` from https://jsoup.org/download and store it in `lib`
    - Download package `htmlparser1_6_20060610.zip` from https://sourceforge.net/projects/htmlparser/files/htmlparser/1.6/htmlparser1_6_20060610.zip/download?use_mirror=altushost-swe&modtime=1149940066&big_mirror=0, extract it and store `htmlparser.jar` in `lib`.

### Set up the environment
3. Set up the environment variable
    - `CATALINA_HOME` = "your tomcat path in this root directory"
    - `JAVA_HOME` = "Your jdk path (OpenJDK 11 is used in this project)"

### Set up Eclipse project
4. Download "Eclipse IDE for Enterprise Java and Web Developers" from https://www.eclipse.org/downloads/packages/release/2022-12/r if you have not, and open the application
5. Create a new workspace for this project.
6. Click File > Import, then choose General > Existing Projects into Workspace and click Next
7. Click Browse and find the repository of where `/COMP4321WebApp` is located at. You should see the project "WebApp" showing up in the projects box. Select "WebApp" and click Finish. 
8. In the Servers view, click the link "No servers are avilable. Click this link to create a new server...".
9. Choose Apache > Tomcat v10.1 Server, then click Next.
10. Click Browse and find the Tomcat directory you have downloaded in the root directory. Then click Finish. You can see there is a new Tomcat server added in the Servers view

### Run the Spider
11. Locate `Spider.java` at `/COMP4321WebApp/src/main/java/com/comp4321`. Right click > Run As > Java Application. It will start crawling the pages. The database file should locate at `/COMP4321WebApp/comp4321.db`. 
    - If it is not, right click `Spider.java` > Run As > Run Configurations. Choose Java Application > Spider, then swithc to the Arugments tab and change the working directory to `${workspace_loc:WebApp}` for MacOS (Not sure about Linux/Window). Click Apply and Run.

### Run the Search Engine Web interface
12. Locate the `search.jsp` file in `/COMP4321WebApp/src/main/webapp/WEB-INF/webapp`. Right click > Run As > Run On Server. Then choose tomcat and check "Always use this server when running this project"
13. Right click `search.jsp` > Run As > Run Configurations. Choose Apache Tomcat > the tomcat server. Switch to the Arguments tab and change the working directory by selecting "Other" and fill in `${workspace_loc:WebApp}` for MacOS (Not sure about Linux/Window). Click Apply and Run.
14. The website can be accessed at http://localhost:8080/WebApp/search.jsp

## User Manual for Search Engine web interface
### Search from input query
Fill in the input query. Then click the magnifying glass button to search. The result will show up.
### Get Similar Pages
Click the "Get Similar Pages" button, and it will submit a query by adding the most frequent 5 keywords to the original query of this result (instead of the query in the input box if changed). You will get the similar pages afterward.
### View all stemmed words and add to query.
1. Click on "View All Stemmed Words" to check all the stemmed words. 
2. Click "Add" if you want to add this keyword to the query.

## Directory preview after completing all steps
```
.
├── COMP4321WebApp
│   ├── comp4321.db
│   ├── src
│   │   └── main
│   │       ├── java
│   │       │   └── com
│   │       │       └── comp4321
│   │       │           ├── Database.java
│   │       │           ├── Indexer.java
│   │       │           ├── Page.java
│   │       │           ├── Parser.java
│   │       │           ├── Porter.java
│   │       │           ├── SearchEngine.java
│   │       │           ├── Spider.java
│   │       │           ├── URLQueue.java
│   │       │           └── Utility.java
│   │       └── webapp
│   │           ├── META-INF
│   │           │   └── MANIFEST.MF
│   │           ├── WEB-INF
│   │           │   ├── classes
│   │           │   │   └── com
│   │           │   │       └── comp4321
│   │           │   │           ├── Database.class
│   │           │   │           ├── Indexer.class
│   │           │   │           ├── NewString.class
│   │           │   │           ├── Page.class
│   │           │   │           ├── Parser.class
│   │           │   │           ├── Porter.class
│   │           │   │           ├── SearchEngine.class
│   │           │   │           ├── Spider.class
│   │           │   │           ├── URLQueue.class
│   │           │   │           └── Utility.class
│   │           │   └── lib
│   │           │       ├── htmlparser.jar
│   │           │       ├── jsoup-1.15.4.jar
│   │           │       └── sqlite-jdbc-3.41.0.0.jar
│   │           └── search.jsp
│   └── stopwords.txt
├── apache-tomcat-10.1.8 (A directory)
├── readme.md
└── report.pdf
```

## Files for submission

1. Report `report.pdf`
2. Readme file for instructions `readme.md`
3. Source folder `COMP4321WebApp`
