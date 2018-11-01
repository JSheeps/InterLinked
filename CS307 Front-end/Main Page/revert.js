"use strict";
var table;

$(document).ready( () => {
	table = new Table("#revertablePlaylistTable", "Revertable Playlists", 2, null, "Playlist Name");
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
            var img = "<img src="revert playlists.png"/>";
            for (var i = 0; i < playlists.length; i++) {
                var playlist = playlists[i];
                table.addRow(
                    null,
                    "<a class='black' onclick=\"revertPlayList(" + playlist.id + ");\">Revert</a>",
                    playlist.name
                );
            }
	});

}
