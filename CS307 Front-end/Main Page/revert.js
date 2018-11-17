"use strict";
var table;
const iconWidth = 15;
const iconHeight = 15;

$(document).ready( () => {
	table = new Table("#revertablePlaylistTable", "Revertable Playlists", null, "Playlist Name");
	viewRevertList();
});



function viewRevertList() {
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
                    "<a><img onclick='revertPlayList(" + playlist.id + ")' src='revert playlists.png' alt='Revert' width='" + iconWidth + "' height='" + iconHeight + "'></a>",
                    playlist.name
                );
            }
	});
}

function revertPlayList(id) {
	var revertButton = table.tbody.find("#" + id).children().eq(0);
	console.log(revertButton);
	var oldHtml = revertButton.html();
	revertButton.html("<img src='loading.png' alt='Loading...' width='" + iconWidth + "' height='" + iconHeight + "'>");

    serverRevert(id).done( (result) => {
        if (result.error) {
			genericErrorHandlers(result.error);
			alert(result.error);
		} else {
			if (result.result) {
				alert("Playlist reverted")
			} else {
				alert(result);
				console.log(result);
			}
		}
		revertButton.html(oldHtml);
	});
}

