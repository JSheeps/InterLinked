"use strict";
var pageContents;

$(document).ready( () => {
	pageContents = $("#mainContents");
	viewImportList("#playlistTable");
});

function viewImportList(tableSelector) {
	var platformID = "Spotify";
	var table = pageContents.find(tableSelector);
	table.find("tr:gt(0)").remove();
	table = pageContents.find(tableSelector + " tr:last");
	
	getImportListFromServer(platformID).done( (playlists) => {
		console.log(playlists);
		var error = playlists.error;
		if (error) {
			if (error == "Unknown Error: Unauthenticated: User needs to log in to service");
			grantServerAccessRedirect();
			return;
		}
		if (playlists.length == 0) {
			table = table.after("<tr><td></td><td>No playlists on Accounts</td></tr>");
			return;
		}
		
		for (var i = playlists.length - 1; i >= 0; i--) {
			var str = "<tr>";
			var playlist = playlists[i];
			str += "<td><a onclick=\"importList('" + platformID + "', '" + playlist.id + "').done(() => viewImportList('" + tableSelector + "'));\">Import</a></td>"
			str += "<td>" + playlist.name + "</td>";
			str += "</tr>";
			importList('Spotify', 'id4');
			// console.log(str);
			table = table.after(str);
		}
		
	});
}