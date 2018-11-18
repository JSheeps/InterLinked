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
			debugger;
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
				{id: "import" + playlist.name },
				"<a class='black' onclick=\"importPlayList('" + platformID + "', '" + playlist.name + "');\">Import</a>",
				playlist.name
			);
		}
	});
}

function importPlayList(platformID, playlistName, force = false) {
	var button = table.tbody.children("[id='import" + playlistName + "']").children().eq(0);
	var oldHtml = button.html();
	
	button.html("<a class='black'>Importing...</a>");
	
	importList(platformID, playlistName, force).done( (result) => {
		if (result.error) {
			if (result.error == "Server Error: Playlist already exists in database (to import anyway, send query: force)") {
				var choice = confirm("This is already in the data base. Would you like to overwrite what is already imported?");
				if (choice)
					importPlayList(platformID, playlistName, true);
				else
					alert("Playlist already exists and user does not wish to overwrite it");
			} else			
				alert("Could not import playlist.\n" + result.error);
		} else
			alert("Imported Successfully!");
		
		
		button.html(oldHtml);
	});
}