"use strict";
var table;

$(document).ready( () => {
	table = new Table("#exportablePlaylistTable", "Exportable Playlists", null, "Playlist Name");
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

function exportPlayList(id) {
	var platformSelectors = $("#platformSelect").children();
	var selected = platformSelectors.filter("input[name='platform']:checked");
	
	var platform = selected.val();
	if (platform == undefined) {
		alert("Must pick a platform to export to");
		return;
	}
	
	
	exportPlaylist(id, platform).done( (response) => {
		genericErrorHandlers(response.error);
		if (response.error) {
			alert(response.error);
			return;
		}
		
		if (response.result) {
			alert("Export Successful!");
			return;
		} else {
			alertText =
				"Could not export every song\n" +
				"These songs could not be exported:";
			
			for (var i = 0; i < response.songs; i++)
				alertText += "\n" + response.songs[i];
			
			alert(alertText);
			console.log(response);
			return;
		}
		
		
	});
}