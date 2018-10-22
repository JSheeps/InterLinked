const url = "/";

function setAuthToken(token) {
	createCookie("accessToken", token);
}

function getAuthToken() {
	return readCookie("accessToken");
}

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
	var authToken = getAuthToken();
	if (authToken) {
		myData.authenticate = authToken;
	}
	
	return $.ajax(url + "data", {
		data: myData,
		dataType: "jsonp"
	});
}

// function to simulate ajax call.
function simulateAjax(obj) {
	return {
		done: function(resolveFunction) { resolveFunction(obj); return this; },
		fail: function(errorFunction) { return this; }
	};
}