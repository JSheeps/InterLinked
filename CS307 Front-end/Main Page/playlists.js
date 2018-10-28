"use strict";

$(document).ready( () => {
	viewPlaylists("#playlistTable");
	
	// Add event listener to search field
	$("input#search").on("enterPressed", search);
	
	$("input").on("keyup", function (event) {
		if (event.which == 13)
			$(this).trigger("enterPressed");
	});
	
});

function viewPlaylists(tableSelector) {
	var table = clearTable(tableSelector);
	tableText(table, "Loading...");
	
	getPlaylistsFromServer().done( (playlists) => {
		/*playlists = [
			{ name: "test" },
			{ name: "second" },
			{ name: "third" },
			{ name: "fourth" }
		];*/
		table = clearTable(tableSelector);
		if (playlists.error) {
			if (playlists.error == "NotLoggedInToService: User needs to log in to streaming service") {
				grantServerAccessRedirect();
				return;
			}
			tableText(table, playlists.error);
			return;
		}
		if (playlists.length == 0) {
			tableText(table, "No Playlists Imported");
			return;
		}
		
		console.log(playlists);
		var html = "";
		html += "<tr class='sorter'><td onclick='sort(0, \"" + tableSelector + "\")' class='clickable'><u>"
		html += "Playlist Names";
		html += "</u></td></tr>";
		
		for (var i = playlists.length - 1; i >= 0; i--) {
			var playlist = playlists[i];
			var str = "<tr><td sortData='" + playlist.name + "'>" + playlist.name + "</td></tr>";
			html += str;
		}
		
		table.after(html);
	});
}

function compare(a, b, ascending) {
	if (a == b) return 0;
	if (a < b) {
		return ascending ? -1 : 1;
	}
	return ascending ? 1 : -1;
}

var orderBy;
function sort(column, id) {
	var table = $(id);
	var tbody = table.children();
	var rows = tbody.children();
	
	var ascending;
	if (orderBy == column) {
		var ascending = tbody.attr("order");
		if (ascending == undefined)
			ascending = true;
		else if (ascending == "asc")
			ascending = false;
		else
			ascending = true;
	} else {
		orderBy = column;
		ascending = true;
	}
	console.log(ascending);
	
	tbody.find("tr:gt(1)").sort( (a, b) => {
		return compare(
			$(a).children().eq(column).attr("sortData"),
			$(b).children().eq(column).attr("sortData"),
			ascending
		);			
	}).appendTo(tbody);
	
	tbody.attr("order", (ascending ? "asc" : "desc"));
}

function tableText(table, text) {
	return table.after("<tr><td><i>" + text + "</i></td></tr>");
}

function search() {
	var searchText = $("#search")[0].value;
	if (searchText.length == 0) {
		alert("No text to search");
	}
	
	serverSearch(searchText).done( (songs) => {
		if (songs.error) {
			alert(songs.error);
			return;
		}
		
		var searchResults = $("#searchResults");
		searchResults.empty();
		console.log(songs);
		for (var i = 0; i < songs.length; i++) {
			var result = songs[i];
			var resultString = "";
			if (result.title) {
				resultString += result.title;
				
				if (result.artist)
					resultString += ", by " + result.artist;
			}
			
			if (resultString.length != 0) {
				searchResults.append("<p class='black'>" + resultString + "</p>");
				
				if (result.SpotifyURL) {
					searchResults.append("<a class='black' target='_blank' href=" + result.SpotifyURL + ">Spotify</a>");
				}
			}
			
		}
	});
}