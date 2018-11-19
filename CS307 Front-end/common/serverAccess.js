"use strict";
const url = "/";

function setAuthData(userName, token) {
	localStorage.setItem("username", userName);
	localStorage.setItem("authToken", token);
}

function clearAuthData() {
	localStorage.removeItem("username");
	localStorage.removeItem("authToken");
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

function serverForgetPassword(username) {
	return sendMessage({
		forgotPassword: username
	});
}

function serverResetPassword(resetToken, newPassword) {
	return sendMessage({
		resetToken: resetToken,
		newPassword: newPassword
	});
}

function serverChangePassword(username, password, newPassword) {
	return sendMessage({
		changePassword: username,
		password: password,
		newPassword: newPassword
	});
}

function getPlaylistsFromServer() {
	return sendMessage({
		get: "playlists"
	});
}

function serverGetSongs(playlistID) {
	return sendMessage({
		playlist: playlistID
	});
}

function serverShare(id) {
	return sendMessage({
		share: id
	});
}

function serverRevert(id) {
	return sendMessage({
		revert: id
	});
}

function serverGetFriendPlaylist(shareCode) {
	return sendMessage({
		importshare: shareCode
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

function serverAddSong(songQuery, playlistID) {
	return sendMessage({
		add: playlistID + " " + songQuery,
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
	var authData = getAuthData();
	if (authData.authToken) {
		myData.authenticate = authData.authToken;
		myData.user = authData.username;
	}
	
	return $.ajax(url + "data", {
		data: myData,
		dataType: "jsonp"
	});
}