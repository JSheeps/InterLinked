"use strict";

var table;
var friendPlaylistInput;

var searchResults = null;
var searchedSong = "";
var localPlaylists = null;

$(document).ready( () => {
	table = new Table("#playlistTable", "My Playlists", null, "Playlist Name", null, null, null);
	friendPlaylistInput = $("#addFromFriend");
	viewPlaylists();
	
	// Add event listener to search field
	$("input#search").on("enterPressed", search);
	friendPlaylistInput.on("enterPressed", addFriendPlaylist);
	
	$("input").on("keyup", function (event) {
		if (event.which == 13)
			$(this).trigger("enterPressed");
	});
	
});

// Local playlist management
function removeLocalPlaylist(i) {	
	localPlaylists[i] = localPlaylists[localPlaylists.length - 1];
	localPlaylists.pop();
}

function getLocalPlaylistIndex(id) {
	for (var i = 0; i < localPlaylists.length; i++) {
		if (localPlaylists[i].id == id) {
			return i;
		}
	}
	
	throw "Can't find playlist with id: " + id;
}

// Viewing playlists
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
		var rowNum = i + 2;
		table.addRow(
			{ id: playlist.id },
			"<a class='black' state='closed' id='showSongsButton" + playlist.id + "' onclick='viewSongs(\"" + playlist.id + "\")'>[+]</a>",
			playlist.name,
			(searchedSong.length != 0 ? "<a class='black' onclick=\"addSearchedSong(" + playlist.id + ");\">Add Song</a>" : ""),
			"<a class='black' onmousedown=\"sharePlayList(" + playlist.id + ");\">Share,</a>",
			"<a class='black' onclick=\"removePlayList('" + playlist.id + "');\">Remove</a></td>"
		);
	}
	
	localPlaylists = playlists;
}

function removePlayList(id) {
	var row = table.getRowByID(id);
	
	serverRemovePlaylist(id).done( (result) => {
		if (result.error) {
			genericErrorHandlers(result.error);
			alert(result.error);
		}
		if (result.result) {
			removeLocalPlaylist(getLocalPlaylistIndex(id));
			viewPlaylists(false);
			alert("Successfully removed");
		} else {
			alert("Failed to remove playlist");
			console.log(result);
		}
	});
}

function viewSongs(id) {
	var playlist =  localPlaylists[getLocalPlaylistIndex(id)];
	if (playlist.songs) {
		expandSongs(id);
		return;
	}
	
	var row = table.getRowByID(id);
	var expandButton = row.children().children().eq(0);
	console.log(expandButton[0].onclick);
	expandButton[0].onclick = null;
	expandButton.text("[...]");
	
	serverGetSongs(id).done( (result) => {
		if (result.error) {
			genericErrorHandlers(result.error);
			alert(result.error);
			return;
		}
		
		playlist.songs = result;
		expandSongs(playlist, row, expandButton);
		console.log(result);
	});
}

function expandSongs(playlist, row = null, expandButton = null) {
	songValuesInit(row, expandButton);
	
	expandButton.text("[-]");
	expandButton[0].onclick = function(event) { collapseSongs(playlist, row, expandButton); };
	
	var songs = playlist.songs;
	for (var i = 0; i < songs.length; i++) {
		var song = songs[i];
		console.log(song);
	}
	
	//console.log(playlist);
	//console.log(row);
	//console.log(expandButton);
}

function collapseSongs(playlist, row = null, expandButton = null) {
	songValuesInit(row, expandButton);
	
	expandButton.text("[+]");
	expandButton[0].onclick = function(event) { expandSongs(playlist, row, expandButton); };
}

function songValuesInit(row, expandButton) {
	if (row == null)
		row = table.getRowByID(playlist.id);
	
	if (expandButton == null)
		expandButton = row.children().children().eq(0);
}

// search functionality
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

function addSearchedSong(playlistID) {
	serverAddSong(searchedSong, playlistID).done( (result) => {
		if (result.error) {
			genericErrorHandlers(result.error);
			alert(result.error);
			return;
		}
		console.log(result);
	});
}


// Sharing functionality
function sharePlayList(i) {
	var row = table.getRow(i);
	var id = row.attr("id");
	
	serverShare(id).done( (result) => {
		if (result.error) {
			genericErrorHandlers(result.error);
			
			alert(result.error);
			return;
		}
		
		if (result.result) {
			var shareField = row.children().eq(1);
			var shareBoxID = "shareBox" + id;
			shareField.html(
				"<div class='tooltip'>" +
					"<input type='text' id='" + shareBoxID + "' class='tooltip sharebox' onmouseout='shareBoxOut(\"" + shareBoxID + "\")' onmouseup='shareBoxCopy(\"" + shareBoxID + "\")' readonly>" +
				"</div>"
			);
			
			shareField = shareField.children().children();
			shareField.val(result.share);
		} else {
			alert(result);
			console.log(result);
		}
	});
}

function shareBoxOut(id) {
	var shareBoxText = $("#" + id + "text");
	shareBoxText.remove();
}

function shareBoxCopy(id) {
	id = id.toString();
	var shareField = $("#" + id);
	shareField.focus();
	shareField.select();
	var tooltipText = (document.execCommand('copy') == false ?
		"Could not copy to clipboard" :
		"Copied to clipboard");
	
	shareField.after(
		"<span class='tooltiptext' id='" + id + "text'>" +
			tooltipText +
		"</span>"
	);
}

function addFriendPlaylistClear() {
	friendPlaylistInput.select();
}

function addFriendPlaylist() {
	var shareCode = friendPlaylistInput.val();
	if (shareCode.length == 0) {
		alert("No share code provided");
		return;
	}
	
	serverGetFriendPlaylist(shareCode).done( (result) => {
		if (result.error) {
			genericErrorHandlers(result);
			alert(result.error);
			return;
		}
		
		if (result.result == true) {
			viewPlaylists();
			alert("Friend's playlist imported");
			return;
		}
		console.log(result);
	});
}