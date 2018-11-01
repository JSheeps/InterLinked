"use strict";
var table;

$(document).ready( () => {
	table = new Table("#importablePlaylistTable", "Importable Playlists", 2, null, "Playlist Name");
	viewImportList();
});

function viewImportList() {
	var platformID = "Spotify";
	table.loading();
	
	getImportListFromServer(platformID).done( (playlists) => {
		table.clear();
		
		var error = playlists.error;
		if (error) {
			genericErrorHandlers(error);
			table.text(error);
			return;
		}
		if (playlists.length == 0) {
			table.text("No playslists on accounts");
			return;
		}			
		
		console.log(playlists);
		table.makeSortRow();
		for (var i = 0; i < playlists.length; i++) {
			var playlist = playlists[i];
			table.addRow(
				null,
				"<a class='black' onclick=\"importPlayList('" + platformID + "', '" + playlist.name + "');\">Import</a>",
				playlist.name
			);
		}
	});
}

function importPlayList(platformID, playlistName) {
	importList(platformID, playlistName).done( (result) => {
		if (result.error) {
			alert("Could not import playlist.\n" + result.error);
			return;
		}
		console.log(result);
		alert("Imported Successfully!");
		viewImportList();
	});
}