const url = "http://127.0.0.1/";

// objects to simulate server output
var playlists = [
	{ name: "Playlist1", id: "id1" },
	{ name: "Playlist2", id: "id2" }
];

var importList = [
	{ name: "Other list"	},
	{ name: "Other other list" }
];

function serverLogin(username, password) {
	return sendMessage({
		login: "login",
		username: username,
		password: password
	});
	return simulateAjax({ accessToken: "nice" });
}

function serverSignup(username, password) {
	return sendMessage({
		signup: "signup",
		username: username,
		password: password
	});
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
		data: myData,
		dataType: "jsonp"
	});
}

function toAuthToken(uName, pWord) {
	return uName + ":" + pWord;
}

// function to simulate ajax call.
function simulateAjax(obj) {
	console.log(obj);
	return {
		done: function(resolveFunction) { resolveFunction(obj); return this; },
		error: function(errorFunction) { return this; }
	};
}