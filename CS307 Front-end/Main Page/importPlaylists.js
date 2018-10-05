"use strict";
var pageContents;

$(document).ready( () => {
	pageContents = $("#mainContents");
	viewImportList("#playlistTable");
});

function viewImportList(tableSelector) {
	var table = pageContents.find(tableSelector + " tr:last");
	
	getImportListFromServer().done( (playlists) => {
		console.log(playlists)
		if (playlists.error) {
			console.log(playlist.error);
			return;
		}
		if (playlists.length == 0) {
			table = table.after("<tr><td>No playlists</td></tr>");
			return;
		}
		
		for (var i = playlists.length - 1; i >= 0; i--) {
			var playlist = playlists[i];
			var str = "<tr><td>" + playlist.name + "</td></tr>";
			table = table.after(str);
		}
	}).error( (error) => {
		console.log(error);
	});
}