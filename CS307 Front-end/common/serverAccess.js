"use strict";
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

function importList(platformID, playlistID, force = false) {
	var data = {
		import: platformID,
		playlist: playlistID,
	}
	
	if (force)
		data.force = true;
	
	console.log (data);
	
	return sendMessage(data);
}

function mergeLists(ids, playlistName) {
	var idString = ids[0];
	for (var i = 1; i < ids.length; i++)
		idString += ", " + ids[i];
	
	return sendMessage({
		merge: idString,
		name: playlistName
	});
}

function exportPlaylist(id, platform) {
	return sendMessage({
		export: id,
		platformID: platform
	});
}

function serverRemovePlaylist(id) {
	return sendMessage({
		remove: id
	});
}

function serverSearch(searchText) {
	return sendMessage({
		search: searchText
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