"use strict";
var table;

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
            //var img = "<img src=\"revert playlists.png\"/>";

            for (var i = 0; i < playlists.length; i++) {
                var playlist = playlists[i];
                table.addRow(
                    null,
                    "<a class='black' onclick=\"revertPlayList(" + playlist.id + ");\"><img src=\"revert playlists.png\"/ alt=\"Revert\" width=\"15\" height=\"15\"></a>",
                    playlist.name
                );
            }
	});
}

function revertPlayList(id) {
    var row = table.getRowByID(id);

    serverRevert(id).done( (result) => {
        if (result.error) {
        genericErrorHandlers(result.error);

        alert(result.error);
        return;
    }

    if (result.result) {
        alert("Playlist reverted")
    } else {
        alert(result);
        console.log(result);
    }
});
}

