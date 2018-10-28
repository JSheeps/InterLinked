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
		for (var i = playlists.length - 1; i >= 0; i--) {
			var playlist = playlists[i];
			var str = "<tr><td>" + playlist.name + "</td></tr>";
			table = table.after(str);
		}
	});
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
			
			if (resultString.length != 0)
				searchResults.append("<p class='black'>" + resultString + "</p>");
			
		}
	});
}