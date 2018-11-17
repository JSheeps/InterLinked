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
				{ id: playlist.id },
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
	
	var exportButton = table.tbody.find("#" + id).children().eq(0);
	var oldHtml = exportButton.html();
	exportButton.html("<a class='black'>Exporting...</a>");
	
	
	exportPlaylist(id, platform).done( (response) => {
		if (response.error) {
			genericErrorHandlers(response.error);
			alert(response.error);
		} else {
			console.log(response);
			if (response.result) {
				alert("Export Successful!");
			} else {
				alertText =
					"Could not export every song\n" +
					"These songs could not be exported:";
				
				for (var i = 0; i < response.songs; i++)
					alertText += "\n" + response.songs[i];
				
				alert(alertText);
			}
		}
		
		exportButton.html(oldHtml);
	});
}