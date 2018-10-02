const url = "127.0.0.1/";

// objects to simulate server output
var playlists = [
	{ name: "Playlist1" },
	{ name: "Playlist2" }
];

var importList = [
	{ name: "Other list"	},
	{ name: "Other other list" }
];

function serverLogin(username, password) {
	var authToken = toAuthToken(username, password);
	console.log(authToken);
	/* return sendMessage({
		login: authToken
	}); */
	return simulateAjax({ accessToken: "nice" });
}

function getPlaylistsFromServer() {
	/*	return sendMessage({
		get: "playlists"
	});
	*/
	console.log(playlists);
	return simulateAjax(playlists);
}

function getImportListFromServer(platformID) {
	/* return sendMessage({
		import: platformID;
	});
	*/
	return simulateAjax(importList);
}

function importList(platformID, playlistID) {
	/* return sendMessage({
		import: platformID,
		playlist: playlistID;
	});
	*/
	return simulateAjax("success");
}

function mergeLists(platformID, playlistID1, playlistID2) {
	/* return sendMessage({
		merge: playlistID1,
		playlist2: playlistID2
	});
	*/
	return simulateAjax("success");
}

function sendMessage(myData) {
	return $.ajax(url + "data", {
		data: myData
	});
}

function toAuthToken(uName, pWord) {
	return uName + ":" + pWord;
}

// function to simulate ajax call.
function simulateAjax(obj) {
	return {
		done: function(resolveFunction) { resolveFunction(obj); return this; },
		error: function(errorFunction) { return this; }
	};
}