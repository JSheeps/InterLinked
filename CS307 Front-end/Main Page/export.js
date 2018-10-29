"use strict";

$(document).ready( () => {
	viewExportablePlaylists("#exportablePlaylistTable");
});

function viewExportablePlaylists(sel) {
	var table = clearTable(sel);
	tableText(table, "Loading...");
	
	getPlaylistsFromServer().done( (playlists) => {
		table = clearTable(sel);
		if (playlists.error) {
			if (playlists.error == "NotLoggedInToService: User needs to log in to streaming service") {
				grantServerAccessRedirect();
				return;
			}
			tableText(table, playlists.error);
			return;
		}
		
		if (playlists.length == 0) {
			tableText(table, "No Playlists Imported");
			return;
		}
		
		console.log(playlists);
		for (var i = playlists.length - 1; i >= 0; i--) {
		    var str = "<tr>";
            var playlist = playlists[i];
            str += "<td><a class='black' onclick=\"exportPlayList('" + playlist.id + "');\">Export</a></td>"
            str += "<td>" + playlist.name + "</td>";
            str += "</tr>";
            table = table.after(str);
		}
	});
}

function tableText(table, text, priorText="") {
	return table.after("<tr><td>" + priorText + "</td> <td><i>" + text + "</i></td></tr>");
}