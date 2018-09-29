"use strict";

const PLAYLISTS = 1;
const IMPORT = 2;
const EXPORT = 3;
const MERGE = 4;

var state = 0;
var pageContents;

var playlists = [
	{ name: "Playlist1", songs: [ 
		{ name: "song 1", artist: "artist 1", source: "Spotify" }
		]
	}
];

$(document).ready( () => {
	pageContents = $("#mainContents");
	console.log(pageContents);
	viewPlaylists();
	updatePage();
});

function viewPlaylists() {
	if (state == PLAYLISTS)
		return;
	
	state = PLAYLISTS;
	clearContents();
	updatePage();
	
	var playlists = getPlaylistsFromServer();
	for (let playlist of playlists) {
		console.log(playlist);
		pageContents.append($("<div class='playlist'><p>" + playlist.name + "</p></div>"));
	}
}

function viewImportPlaylists() {
	if (state == IMPORT)
		return;
	
	state = IMPORT;
	clearContents();
	updatePage();
}

function viewExportPlaylists() {
	if (state == EXPORT)
		return;
	
	state = EXPORT;
	clearContents();
	updatePage();
}

function viewMergePlaylists() {
	if (state == MERGE)
		return;
	
	state = MERGE;
	clearContents();
	updatePage();
}

function clearContents() {
	pageContents.empty();
}

function updatePage() {
	var string;
	switch (state) {
		case PLAYLISTS:
			string = "Playlists";
			break;
		case IMPORT:
			string = "Import";
			break;
		case EXPORT:
			string = "Export";
			break;
		case MERGE:
			string = "Merge";
			break;
		default:
			string = "Error";
			break;
	}
	
	$("p#textStatus").html(string);
}

// Server communications
function getPlaylistsFromServer() {
	return playlists;
}