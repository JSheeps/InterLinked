const url = "/";

// objects to simulate server output
var playlists = [
	{ name: "Playlist1", id: "id1" },
	{ name: "Playlist2", id: "id2" }
];

var importListData = [
	{ name: "Other list", id: "id3" },
	{ name: "Other other list", id: "id4" }
];

function serverLogin(username, password) {
	return sendMessage({
		login: "login",
		username: username,
		password: password
	});
}

function serverSignup(username, password, email) {
	return sendMessage({
		signup: "signup",
		username: username,
		password: password,
		email: email
	});
}

function getPlaylistsFromServer() {
	return sendMessage({
		get: "playlists"
	});
}

function getImportListFromServer(platformID) {
	return sendMessage({
		import: platformID
	});
}

function importList(platformID, playlistID) {
	return sendMessage({
		import: platformID,
		playlist: playlistID
	});
}

function mergeLists(platformID, playlistID1, playlistID2) {
	return sendMessage({
		merge: playlistID1,
		playlist2: playlistID2
	});
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
	return {
		done: function(resolveFunction) { resolveFunction(obj); return this; },
		fail: function(errorFunction) { return this; }
	};
}