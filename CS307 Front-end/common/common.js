"use strict";

function displayUsername(){
	$("#displayUsername").html(getUserName());
}


function getUserName() { return localStorage.getItem("username"); }
function getAuthToken() { return localStorage.getItem("authToken"); }

function getAuthData() {
	return {
		username: getUserName(),
		authToken: getAuthToken()
	}
}

function removeAuthData() {
	localStorage.removeItem("username");
	localStorage.removeItem("authToken");
}

// Page redirects
function logoutRedirect() {
	removeAuthData();
	redirect("/");
}

function loginRedirect() {
	redirect("/Main Page/viewPlaylists.html");
}

function signupRedirect() {
	redirect("/Login Page/signup.html");
}

function playlistRedirect() {
	redirect("/Main Page/viewPlaylists.html");
}

function importRedirect() {
	redirect("/Main Page/import.html");
}

function exportRedirect() {
	redirect("/Main Page/export.html");
}

function mergeRedirect() {
	redirect("/Main Page/merge.html");
}

function revertRedirect() {
    redirect("/Main Page/revert.html")
}

function grantServerAccessRedirect() {
	redirect("/SelectService Page/SelectService.html");
}

function redirect(URI) {
	window.location.href = URI;
}