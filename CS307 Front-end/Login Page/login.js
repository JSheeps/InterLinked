"use strict";

// Global variables to reduce Querying

var userNameField;
var passwordField;

function userNameError(msg) {
	alert(msg);
}

function passWordError(msg) {
	alert(msg);
}

function login() {
	var userName = userNameField.val();
	var password = passwordField.val();
	
	if (userName.length == 0) {
		userNameError("Need to input a username");
		return;
	}
	
	if (password.length == 0) {
		passWordError("Need to input a password");
		return;
	}
	
	var buttons = $("div.btnRef");
	
	var onclicks = [];
	var texts = [];
	for (var i = 0; i < buttons.length; i++) {
		var button = buttons[i];
		onclicks.push(button.onclick);
		texts.push(button.innerHTML);
		button.innerHTML = "Loading...";
		button.onclick = null;
	}
	
	
	serverLogin(userName, password).done( (accessToken) => {
		if (!accessToken.result) {
			alert("Error: Invalid login");
		} else {
			var token = accessToken.authenticate;
			setAuthData(userName, token);
			playlistRedirect();
		}
		
		for (var i = 0; i < buttons.length; i++) {
			buttons[i].onclick = onclicks[i];
			buttons[i].innerHTML = texts[i];
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
		if (userNameField.val().length != 0)
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