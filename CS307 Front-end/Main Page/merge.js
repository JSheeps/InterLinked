"use strict"
var table;
var mergeListNameBox;

$(document).ready( () => {
	table = new Table("#mergeablePlaylistTable", "Mergeable Playlists", 2, null, "Playlist Name");
	viewMergeList();
	mergeListNameBox = $("#mergePlaylistName");
	
	// Add event listener to playlist name field
	mergeListNameBox.on("enterPressed", mergePlaylists);
	
	mergeListNameBox.keyup( function (event) {
		if (event.which == 13)
			$(this).trigger("enterPressed");
	});
});

function viewMergeList() {
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
		
		if (playlists.length < 2) {
			table.text("Needs 2 or more playlists imported");
			return;
		}
		
		table.makeSortRow();
		for (var i = 0; i < playlists.length; i++) {
			var playlist = playlists[i];
			table.addRow(
				{ id: playlist.id },
				"<input type=\"checkbox\">",
				playlist.name
			);
		}
	});
}

function mergePlaylists() {
	var name = mergeListNameBox.val();
	if (name.length == 0) {
		alert("You need to name the playlist");
		return;
	}
	
	if (!verifyName(name)) {
		alert("Invalid playlist name (can only contain alphanumerics)");
		return;
	}
	
	var ids = [];
	for (var i = 2; i < table.rows(); i++) {
		var row = table.getRow(i);
		var checkbox = row.children().find("input[type=checkbox]");
		var value = checkbox.prop("checked");
		if (value) {
			var id = row.attr("id");
			ids.push(id);
		}
	}
	
	if (ids.length <= 1) {
		alert("You need to select more than one playlist to merge.");
		return;
	}
	
	mergeLists(ids, name).done( (result) => {
		if (result.error) {
			genericErrorHandlers(result);
			alert(result.error);
		} else {
			alert("Merge successful!");
			console.log(result);
		}
	});
}

const a = 'a'.charCodeAt(0);
const z = 'z'.charCodeAt(0);
const A = 'A'.charCodeAt(0);
const Z = 'Z'.charCodeAt(0);
const zero = '0'.charCodeAt(0);
const nine = '9'.charCodeAt(0);

function verifyName(name) {
	for (var i = 0; i < name.length; i++) {
		var char = name.charCodeAt(i);
		var alpha = isAlpha(char);
		if (!isLegalCharacter(char))
			return false;
			
	}
	return true;
}

function isLegalCharacter(char) {
	return isAlpha(char) ||
		isNumber(char) ||
		isValidOtherCharacter(char);
		
}

function isAlpha(char) {
	return (char >= a && char <= z) || (char >= A && char <= Z);
}

function isNumber(char) {
	return (char >= zero && char <= nine);
}

function isValidOtherCharacter(char) {
	return "()% ".indexOf(String.fromCharCode(char)) != -1;
}