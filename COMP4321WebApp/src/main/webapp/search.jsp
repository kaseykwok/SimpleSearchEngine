<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ page import="com.comp4321.SearchEngine,
	com.comp4321.Page,
	com.comp4321.Utility,
	java.util.Vector,
	java.util.Map.Entry,
	java.util.List
" %>

<!DOCTYPE html>
<html>
	<head>
		<meta charset="UTF-8">
		<title>COMP4321 Search Engine</title>
		<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css">
		<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@48,700,0,0" />
		<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.min.js"></script>
		<script type="text/javascript">
			function queryInputUpdate(val) {
				var hiddenQuery = document.getElementById('hiddenQuery');
				hiddenQuery.value = val;
			}
			
			function getSimilarPages(val) {
				var form = document.getElementById('queryForm');
				var hiddenQuery = document.getElementById('hiddenQuery');
				var query = '<%= request.getParameter("query")%>';
				hiddenQuery.value = query + " " + val;
				form.submit();
			}
			
			function addToQuery(word) {
				var inputQuery = document.getElementById('inputQuery');
				var hiddenQuery = document.getElementById('hiddenQuery');
				inputQuery.value = inputQuery.value + " " + word;
				hiddenQuery.value = inputQuery.value;
			}
			
			function toggleShowKeywords(btn, id) {
				var keywordTable = document.getElementById(id);
				if (keywordTable.style.display == "none") {
					keywordTable.style.display = "block";
					btn.innerHTML = "Hide All Stemmed Words"
				} else {
					keywordTable.style.display = "none";
					btn.innerHTML = "View All Stemmed Words"
				}
			}
		</script>
		<style>
			.table-fix {
		        overflow-y: auto;
		        max-height: 400px; 
		        overflow-x: hidden;
			}
			.table-fix thead th {
			  position: sticky; 
			  top: 0px;
			}
			table {
			  width: 100%;
			}
		</style>
	</head>
	<body>
		<div class="container-fluid my-5 mx-auto" style="max-width: 900px;">
			<form id="queryForm" method="post" action="search.jsp">
				<h1>COMP4321 Search Engine</h1>
				<div class="p-4 sticky-top bg-light">
					<div class="input-group">				
						<input id="inputQuery" type="text" class="form-control" name="inputQuery" oninput="queryInputUpdate(this.value)" value='<%= request.getParameter("query") == null ? "" : request.getParameter("query") %>' placeholder="Search">
						<div class="input-group-append">					
							<button type="submit" class="btn btn-secondary">
								<span class="material-symbols-outlined">search</span>
							</button>
						</div>
					</div>	
					<input id="hiddenQuery" type="hidden" class="form-control" name="query" value='<%= request.getParameter("query") == null ? "" : request.getParameter("query") %>' placeholder="Search">	
				</div>
			
				<%!
					String getKeywordTableHTML(List<Entry<String, Integer>> frequentWords, int pageId) {
						String tableHTML = "	<div id='keywordTable" + pageId + "' class='row g-0 table-fix' style='display: none'>" +
								"		<table class='table table-sm'>" +
								"			<thead>" +	
								"				<tr class='table-secondary'>" +		
								"					<th scope='col'>Stemmed Keyword</th>" +	
								"					<th scope='col'>Term Frequency</th>" +
								"					<th scope='col'>Add to Query</th>" +
								"				</tr>" +	
								"			</thead>" +
								"			<tbody>";
								
						for(Entry<String, Integer> frequentWord : frequentWords) {
							tableHTML += "				<tr>" +		
								"					<td>" + frequentWord.getKey() +"</td>" +	
								"					<td>" + frequentWord.getValue() + "</td>" +
								"					<td><button type='button' class='btn btn-secondary' onclick='addToQuery(\"" + frequentWord.getKey() + "\")'> Add </button></td>" +
								"				</tr>";
						}
						
						tableHTML += "			</tbody>" +
								"		</table>" +
								"	</div>";
								
						return tableHTML;
					}
				
					String getPageDisplayHTML(Page resultPage) {
						String displayHTML = "<div class='card mb-3 p-3'>" +
								"	<div class='row g-0'>" +
								"		<div class='col-md-2'>" +
								"			<div class='card-body'>" +
								"				<h5 class='card-title'>" + String.format("%.4f", resultPage.similarityScore) + "</h5>" +
								"				<p class='card-text'><small class='text-body-secondary text-secondary'>Out of 100</small></p>" +
								"			</div>" +
								"		</div>" +
								"		<div class='col-md-10'>" +
								"			<div class='card-body'>" +
								"				<h5 class='card-title fw-bold'>" + resultPage.title + "</h5>" +
								"				<a href='" + resultPage.url + "' class='card-text'>" + resultPage.url + "</a>" +
								"				<p class='card-text'>" +
								"					" + Utility.convertDateToString(resultPage.lastModificationDate) + ", " + resultPage.size + "<br>";
						
						int wordCount = 0;
						for (Entry<String, Integer> frequentWord : resultPage.frequentWords) {
							wordCount++;
							displayHTML += frequentWord.getKey() + " " + frequentWord.getValue() + "; ";
							if(wordCount >= 5) break;
						}
		
						displayHTML += "				</p>" +
								"				<p class='card-text'><span class='fw-semibold'>Parent links (at most 10): </span><br>";
						
						if (resultPage.parentLinks.size() == 0) {
							displayHTML += "No parent links";
						} else {						
							for (String parentLink : resultPage.parentLinks) {
								displayHTML += "<a href='" + parentLink + "' class='card-text'>" + parentLink + "</a><br>";
							}
						}
								
						displayHTML += "				</p>" +
								"				<p class='card-text'><span class='fw-semibold'>Child links (at most 10): </span><br>";
						
						if (resultPage.childLinks.size() == 0) {
							displayHTML += "No child links";
						} else {	
							for (String childLink : resultPage.childLinks) {
								displayHTML += "<a href='" + childLink + "' class='card-text'>" + childLink + "</a><br>";
							}
						}
						
						String top5keywords = resultPage.getTop5Keywords();
								
						displayHTML += "				</p>" +
								" 				<button type='button' class='btn btn-secondary' onclick='getSimilarPages(\"" + top5keywords + "\")'> Get Similar Pages </button>" +
								" 				<button type='button' class='btn btn-secondary' onclick='toggleShowKeywords(this, \"keywordTable" + resultPage.pageId + "\")'> View All Stemmed Words </button>" +
								"			</div>" +
								"		</div>" +
								"	</div>" + 
								getKeywordTableHTML(resultPage.frequentWords, resultPage.pageId) +
								"</div>";
								
						return displayHTML;
					}
				%>
				
				
				<div class="pt-4">
					<%
						if (request.getParameter("query") != "" && request.getParameter("query") != null) {
							SearchEngine se = new SearchEngine();
							Vector<Page> resultPages = se.getQueryResult(request.getParameter("query"));
							
							if (resultPages.size() == 0) {
								out.println("<h5>Sorry, no page is found to match the query: [" + request.getParameter("query") + "]</h5>");
							} else {
								out.println("<h5 class='mb-3'>" + resultPages.size() + " page(s) are found matching the query: [" + request.getParameter("query") + "]</h5>");
								for (Page resultPage : resultPages) {
									out.println(getPageDisplayHTML(resultPage));
								}
							}					
						} else {
							out.println("<h5>Your query is empty. Please type your query.<h5>");
						}
					%>
				</div>
			</form>
		</div>
	</body>
</html>