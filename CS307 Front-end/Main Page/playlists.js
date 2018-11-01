"use strict";

var table;
var searchResults = null;
var searchedSong = "";
var localPlaylists = null;

$(document).ready( () => {
	table = new Table("#playlistTable", "My Playlists", null, null, "Playlist Name", null);
	viewPlaylists();
	
	// Add event listener to search field
	$("input#search").on("enterPressed", search);
	
	$("input").on("keyup", function (event) {
		if (event.which == 13)
			$(this).trigger("enterPressed");
	});
	
});

function viewPlaylists(refresh = true) {
	table.loading();
	if (refresh)
		getPlaylistsFromServer().done(fillTable);
	else
		fillTable(localPlaylists);
}

function fillTable(playlists) {
	table.clear();
	if (playlists.error) {
		genericErrorHandlers(playlists.error);
		table.text(playlists.error);
		return;
	}
	if (playlists.length == 0) {
		table.text("No Playlists Imported");
		return;
	}
	
	table.makeSortRow();
	
	for (var i = 0; i < playlists.length; i++) {
		var playlist = playlists[i];
		table.addRow(
			null,
			(searchedSong.length != 0 ? "<a class='black' onclick=\"addSearchedSong(" + playlist.id + ");\">Add Song,</a>" : ""),
			"<a class='black' onclick=\"sharePlayList(" + playlist.id + ");\">Share</a>",
			playlist.name,
			"<a class='black' onclick=\"removePlayList('" + playlist.id + "');\">Remove</a></td>"
		);
	}
	
	localPlaylists = playlists;
}

function search() {
	if (searchResults == null)
		searchResults = new Table("#searchResults", "Search Results", null);
	
	
	var searchText = $("#search").val();
	if (searchText.length == 0) {
		searchResults.clear().text("No text to search");
	}
	
	searchResults.clear().text("Searching...");
	
	serverSearch(searchText).done( (songs) => {
		searchResults.clear();
		if (songs.error) {
			genericErrorHandlers(songs.error);
			
			if (songs.error == "Unknown Error: null") {
				searchResults.text("Song not found");
				searchText = "";
				return;
			}
			alert(songs.error);
			return;
		}
		
		// console.log(songs);
		for (var i = 0; i < songs.length; i++) {
			var result = songs[i];
			var resultString = "";
			if (result.title) {
				resultString += result.title;
				
				if (result.artist)
					resultString += ", by " + result.artist;
			}
			
			if (resultString.length != 0) {
				searchResults.addRow(null, "<p class='black'>" + resultString + "</p>");
				
				if (result.SpotifyURL) {
					searchResults.addRow(null, "<a class='black' target='_blank' href=" + result.SpotifyURL + ">Spotify</a>");
				}
			}
		}
		searchedSong = searchText;
		viewPlaylists(false);
	});
}

function removePlayList(id) {
	serverRemovePlaylist(id).done( (result) => {
		if (result.error) {
			genericErrorHandlers(result.error);
			alert(result.error);
		}
		if (result.result) {
			alert("Success");
			viewPlaylists();
		} else {
			alert("Failed to remove playlist");
			console.log(result);
		}
	});
}

function sharePlayList(id) {
	console.log("Share: " + id);
}

function addSearchedSong(playlistID) {
	
}