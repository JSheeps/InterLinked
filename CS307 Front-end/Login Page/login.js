"use strict";

// Global variables to reduce Querying

var userNameField;
var passwordField;
function field_focus(field, defaultString) {
	// Default behavior
}

function field_blur(field, defaultString) {
	// Default behavior
}

function userNameError(msg) {
	alert(msg);
}

function passWordError(msg) {
	alert(msg);
}

function getUserName() {
	return userNameField[0].value;
}

function getPassword() {
	return passwordField[0].value;
}

function login() {
	var userName = getUserName();
	var password = getPassword();
	
	if (userName.length == 0) {
		userNameError("Need to input a username");
		return;
	}
	
	if (password.length == 0) {
		passWordError("Need to input a password");
		return;
	}
	
	//used for displaying usernames on each page
	localStorage.setItem("username", userName);
	
	serverLogin(userName, password).done( (accessToken) => {
		if (!accessToken.result) {
			alert("Error: Invalid login");
		} else {
			var token = accessToken.authenticate;
			setAuthToken(token);
			playlistRedirect();
		}
	});
}

function signUp() {
	signupRedirect();
}

// check if already has session token
if (getAuthToken())
	playlistRedirect();

$(document).ready( () => {
	// initailize Field variables
	userNameField = $("#userNameInput");
	passwordField = $("#passwordInput");
	
	// Add event listeners to the input fields
	
	var enterPressed = "enterPressed"
	// On enter while in the password field, login
	passwordField.on(enterPressed, login);
	
	// On enter while in the username field, change focus to the password field
	userNameField.on(enterPressed, (event) => {
		if (getUserName().length != 0)
			passwordField.focus();
		else
			userNameError("Need to input a username");
	});
	
	
	// Establish a 'Enter' key listener	
	$("input").on("keyup", function (event) {
		// if the 'Enter' key is pressed
		if (event.which == 13) {
			$(this).trigger("enterPressed");
		}
	});
	
	userNameField.focus();
});