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
	
	if (playlists == null || playlists.length == 0) {
		table.text("No Playlists Imported");
		return;
	}
	
	if (playlists.error) {
		genericErrorHandlers(playlists.error);
		table.text(playlists.error);
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
			"<a class='black shareButton' onmousedown=\"sharePlayList(" + playlist.id + ");\">Share</a>",
			"<a class='black' onclick=\"removePlayList('" + playlist.id + "');\">Remove</a></td>"
		);
	}
	localPlaylists = playlists;
}

function removePlayList(id) {
	var row = table.getRowByID(id);
	var btn = table.tbody.find("#" + id).children().eq(4);
	var oldHtml = btn.html();
	btn.html("<a class='black'>Removing...</a>");
	
	serverRemovePlaylist(id).done( (result) => {
		if (result.error) {
			genericErrorHandlers(result.error);
			alert(result.error);
		} else {
			if (result.result) {
				removeLocalPlaylist(getLocalPlaylistIndex(id));
				viewPlaylists(false);
				alert("Successfully removed");
			} else {
				alert("Failed to remove playlist");
				console.log(result);
			}
		}
		btn.html(oldHtml);
	});
}

function viewSongs(id) {
	var index = getLocalPlaylistIndex(id);
	var playlist =  localPlaylists[index];
	
	var row = table.getRowByID(id);
	var expandButton = row.children().eq(0).children().eq(0);
	
	if (playlist.songTable) {
		expandSongs(playlist, expandButton);
	}
	
	if (playlist.songs) {
		makeSongTable(index);
		expandSongs(playlist, expandButton);
		return;
	}
	
	updateSongTable(index, expandButton, () => {
		expandSongs(playlist, expandButton);
	});
}

function makeSongTable(index) {
	var playlist = localPlaylists[index];
	var id = playlist.id;
	var songTable = {
		val: "<table id='" + id + "songs'></table>",
		span: table.columns
	};
	
	table.addRowAfter("#" + id, { bind: "UP" }, songTable);
	
	var songTable = new Table("#" + id + "songs", null, null, "Title", "Artist");
	playlist.songTable = songTable;
	songTable.table.addClass("subTable");
	
	if (playlist.songs.length == 0) {
		songTable.text("Playlist Empty");
		return;
	}
	
	songTable.makeSortRow("localPlaylists[" + index + "].songTable");
	
	for (var i = 0; i < playlist.songs.length; i++) {
		var song = playlist.songs[i];
		songTable.addRow({id: "i" + i}, "<a class='black' onclick='removeSong(" + i + ", " + id + ")'> Remove </a>", song.title, song.artist);
	}
}

function updateSongTable(index, expandButton = null, afterFunction = () => {}) {
	var playlist = localPlaylists[index];
	var id = playlist.id;
	
	if (expandButton == null) {
		var row = table.getRowByID(id);
		expandButton = row.children().eq(0).children().eq(0);
	}
	
	var oldExpandHtml = expandButton.html();
	var oldClick = expandButton[0].onclick;
	expandButton.html("<a class='black'>[...]</a>");
	expandButton[0].onclick = null;
	
	if (playlist.songTable) {
		var row = playlist.songTable.table.parent().parent();
		row.remove();
		console.log(row);
	}
	
	serverGetSongs(id).done( (result) => {
		expandButton.html(oldExpandHtml);
		expandButton[0].onclick = oldClick;
		if (result.error) {
			genericErrorHandlers(result.error);
			alert(result.error);
			return;
		}
		
		playlist.songs = result;
		makeSongTable(index);
		afterFunction();
	});
}

function expandSongs(playlist, expandButton) {
	playlist.songTable.show();
	
	expandButton.text("[-]");
	expandButton[0].onclick = function(event) { collapseSongs(playlist, expandButton); };
}

function collapseSongs(playlist, expandButton) {
	playlist.songTable.hide();
	
	expandButton.text("[+]");
	expandButton[0].onclick = function(event) { expandSongs(playlist, expandButton); };
}

function removeSong(index, playlistID) {
	var removeButton = $("table#" + playlistID + "songs").find("tr#i" + index).children().eq(0);
	var oldHtml = removeButton.html();
	removeButton.html("<a class='black'> Removing... </a>");
	
	serverRemoveSong(playlistID, index).done( result => {
		if (result.error) {
			genericErrorHandlers(result.error);
			alert(result.error);
			removeButton.html(oldHtml);
		} else {
			console.log(result);
			updateSongTable(
				getLocalPlaylistIndex(playlistID),
				null,
				() => removeButton.html(oldHtml)
			);
		}
	});
}

// search functionality
function search() {
	if (searchResults == null)
		searchResults = new Table("#searchResults", "Search Results", null);
	
	var searchPlatform = $("input[name='platform']:checked").val();
	if (!searchPlatform)
		throw "could not find search platform";
	
	var searchButton = $("#searchbtn");
	searchButton.attr("disabled", "disabled");
	
	var searchText = $("#search").val();
	if (searchText.length == 0) {
		searchResults.clear().text("No text to search");
		searchButton.removeAttr("disabled");
		return;
	}
	
	searchResults.clear().text("Searching...");
	
	serverSearch(searchText, searchPlatform).done( (songs) => {
		searchResults.clear();
		if (songs.error) {
			genericErrorHandlers(songs.error);
			
			if (songs.error == "Server Error: No results" || songs.error == "No results") {
				searchResults.text("Song not found");
				searchText = "";
			} else if (songs.error == "Server Error: AuthToken is not allowed to be null. Use TokenProvider.provideToken if you need to generate one.") {
				searchResults.text("Need to sign in to Google Play Music");
				searchText = "";
			} else {				
				alert(songs.error);
			}
		} else {
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

                    if (result.YoutubeURL) {
                        searchResults.addRow(null, "<a class='black' target='_blank' href=" + result.YoutubeURL + ">Youtube</a>");
                    }
				}
			}
			searchedSong = result.SpotifyID;
			viewPlaylists(false);
		}
		searchButton.removeAttr("disabled");
	});
	
}

function addSearchedSong(playlistID) {
	var addButton = table.tbody.find("#" + playlistID).children().eq(2);
	var oldHtml = addButton.html();
	addButton.html("<a class='black'>Adding song...</a>");
	
	serverAddSong(searchedSong, playlistID).done( (result) => {
		if (result.error) {
			genericErrorHandlers(result.error);
			alert(result.error);
			addButton.html(oldHtml);
			return;
		}
		
		if (result.result) {
			alert("Added song successfully");
			var index = getLocalPlaylistIndex(playlistID);
			var playlist = localPlaylists[index];
			if (playlist.songTable)
				updateSongTable(index);
			addButton.html(oldHtml);
		}
	});
}


// Sharing functionality
function sharePlayList(id) {
	var row = table.getRowByID(id);
	var shareField = row.children("td").eq(3);
	shareField.html("<a class='black'>Sharing...</a>");
	
	serverShare(id).done( (result) => {
		if (result.error) {
			genericErrorHandlers(result.error);
			
			alert(result.error);
			return;
		}
		
		if (result.result) {
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
	friendPlaylistInput.prop("readonly", true);
	var shareCode = friendPlaylistInput.val();
	if (shareCode.length == 0) {
		alert("No share code provided");
		return;
	}
	
	var addFriendPlaylistButton = $("#addPlaylistBtn");
	addFriendPlaylistButton.prop("disabled", true);
	
	serverGetFriendPlaylist(shareCode).done( (result) => {
		if (result.error) {
			genericErrorHandlers(result);
			alert(result.error);
		} else {
			if (result.result == true) {
				viewPlaylists();
				alert("Friend's playlist imported");
			} else {
				alert("Could not find playlist");
			}
		}
		friendPlaylistInput.prop("readonly", false);
		addFriendPlaylistButton.prop("disabled", false);
	});
}