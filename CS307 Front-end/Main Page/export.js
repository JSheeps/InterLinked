"use strict";
var table;

$(document).ready( () => {
	table = new Table("#exportablePlaylistTable", "Exportable Playlists", 2, null, "Playlist Name");
	viewExportablePlaylists();
});

function viewExportablePlaylists() {
	table.loading();
	
	getPlaylistsFromServer().done( (playlists) => {
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
		
		console.log(playlists);
		table.makeSortRow();
		for (var i = 0; i < playlists.length; i++) {
            var playlist = playlists[i];
			table.addRow(
				null,
				"<a class='black' onclick=\"exportPlayList('" + playlist.id + "');\">Export</a>",
				playlist.name
			);
		}
	});
}

function exportPlayList(id, platformID = "Spotify") {
	exportPlaylist(id, platformID).done( (response) => {
		genericErrorHandlers(response.error);
		if (response.error) {
			alert(response.error);
			return;
		}
		
		if (response.length == 0) {
			alert("Export Successful!");
			return;
		}
		console.log(response);
	});
}