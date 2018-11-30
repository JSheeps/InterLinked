"use strict";
var table;

$(document).ready( () => {
	table = new Table("#importablePlaylistTable", "Importable Playlists", null, "Playlist Name");
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
			alert(error);
			return;
		}
		if (playlists.length == 0) {
			table.text("No playslists on accounts");
			return;
		}			
		
		table.makeSortRow();
		for (var i = 0; i < playlists.length; i++) {
			var playlist = playlists[i];
			table.addRow(
				{id: "import" + i },
				"<a class='black' onclick=\"importPlayList(" + i + ",'" + platformID + "', '" + playlist.name + "');\">Import</a>",
				playlist.name
			);
		}
	});
}

function importPlayList(id, platformID, playlistName, callback = null, force = false) {
	var button = table.tbody.children("[id='import" + id + "']").children().eq(0);
	var oldHtml = button.html();
	
	button.html("<a class='black'>Importing...</a>");
	function restoreButton() {
		if (callback) callback();
		button.html(oldHtml);
	}
	
	importList(platformID, playlistName, force).done( (result) => {
		if (result.error) {
			if (result.error == "Server Error: Playlist already exists in database (to import anyway, send query: force)") {
				var choice = confirm("This is already in the data base. Would you like to overwrite what is already imported?");
				if (choice) {
					importPlayList(id, platformID, playlistName, restoreButton, true);
					return;
				} else
					alert("Playlist already exists and user does not wish to overwrite it");
			} else			
				alert("Could not import playlist.\n" + result.error);
		} else
			alert("Imported Successfully!");
		
		restoreButton();
	});
}