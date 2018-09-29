"use strict";

// Global variables to reduce CPU usage
var userNameField;
var passwordField;

function field_focus(field, defaultString) {
	// Default behavior
}

function field_blur(field, defaultString) {
	// Default behavior
}

function login() {
	var userName = userNameField[0].value;
	var password = passwordField[0].value;

	console.log(userName);
	console.log(password);
	
	// TODO: Login to server here
	console.log("Login");
}

$(document).ready( () => {
	// initailize Field variables
	userNameField = $("#userNameInput");
	passwordField = $("#passwordInput");
	
	// Add event listeners to the input fields
	
	var enterPressed = "enterPressed"
	// On enter while in the password field, login
	passwordField.on(enterPressed, login);
	
	// On enter while in the username field, change focus to the password field
	userNameField.on(enterPressed, event => passwordField.focus());
	
	// Establish a 'Enter' key listener	
	$("input").on("keyup", function (event) {
		// if the 'Enter' key is pressed
		if (event.which == 13) {
			$(this).trigger("enterPressed");
		}
	});
	
	
	




});