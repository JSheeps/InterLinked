"use strict";
var pageContents;

$(document).ready( () => {
	pageContents = $("#mainContents");
	viewPlaylists("#playlistTable");
});

function viewPlaylists(tableSelector) {
	var table = pageContents.find(tableSelector + " tr:last");
	
	getPlaylistsFromServer().done( (playlists) => {
		console.log(playlists);
		if (playlists.length == 0) {
			table = table.after("<tr><td><i>No Playlists Imported</i></td></tr>");
		}
		for (var i = playlists.length - 1; i >= 0; i--) {
			var playlist = playlists[i];
			var str = "<tr><td>" + playlist.name + "</td></tr>";
			table = table.after(str);
		}
	});
}