"use strict";
var pageContents;
const plsel = "#importablePlaylistTable";

$(document).ready( () => {
	pageContents = $("#mainContents");
	viewImportList(plsel);
});

function viewImportList(tableSelector) {
	var platformID = "Spotify";
	var table = clearTable(tableSelector);
	tableText(table, "Loading...");
	
	getImportListFromServer(platformID).done( (playlists) => {
		table = clearTable(tableSelector);
		console.log(table);
		var error = playlists.error;
		if (error) {
			if (error == "Unknown Error: Unauthenticated: User needs to log in to service" || error == "NotLoggedInToService: User needs to log in to streaming service") {
				grantServerAccessRedirect();
				return;
			}
			tableText(table, error);
			return;
		}
		if (playlists.length == 0) {
			tableText(table, "No playslists on accounts");
			return;
		}
		
		console.log(playlists);
		for (var i = playlists.length - 1; i >= 0; i--) {
			var str = "<tr>";
			var playlist = playlists[i];
			str += "<td><a class='black' onclick=\"importPlayList('" + platformID + "', '" + playlist.id + "');\">Import</a></td>"
			str += "<td>" + playlist.name + "</td>";
			str += "</tr>";
			table = table.after(str);
		}
	});
}

function importPlayList(platformID, playlistID) {
	importList(platformID, playlistID).done( (result) => {
		if (result.error) {
			alert("Could not import playlist.\n" + result.error);
			return;
		}
		console.log(result);
		viewImportList(plsel);
	});
}

function tableText(table, text, priorText = "") {
	return table.after("<tr><td>" + priorText + "</td> <td><i>" + text + "</i></td></tr>");
}