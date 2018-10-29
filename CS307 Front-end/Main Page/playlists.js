"use strict";

var table;

$(document).ready( () => {
	table = new Table("#playlistTable", "My Playlists", 3, null, "Playlist Name", null);
	viewPlaylists("#playlistTable");
	
	// Add event listener to search field
	$("input#search").on("enterPressed", search);
	
	$("input").on("keyup", function (event) {
		if (event.which == 13)
			$(this).trigger("enterPressed");
	});
	
});

function viewPlaylists(tableSelector) {
	table.clear().text("Loading...");
	
	getPlaylistsFromServer().done( (playlists) => {
		/*playlists = [
			{ name: "test" },
			{ name: "second" },
			{ name: "third" },
			{ name: "fourth" }
		];*/
		
		table.clear();
		if (playlists.error) {
			if (playlists.error == "NotLoggedInToService: User needs to log in to streaming service") {
				grantServerAccessRedirect();
				return;
			}
			table.text(playlists.error);
			return;
		}
		if (playlists.length == 0) {
			table.text("No Playlists Imported");
			return;
		}
		
		console.log(playlists);
		table.makeSortRow();
		
		for (var i = playlists.length - 1; i >= 0; i--) {
			var playlist = playlists[i];
			table.addRow(
				"<a class='black' onclick=\"sharePlayList();\">Share</a>",
				playlist.name,
				"<a class='black' onclick=\"removePlayList('" + playlist.id + "');\">Remove</a></td>"
			);
		}
	});

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
