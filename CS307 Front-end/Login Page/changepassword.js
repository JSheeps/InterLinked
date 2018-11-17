"use strict";

// Global variables to reduce Querying
var usernameField;
var newPasswordField;
var currentPasswordField;

function field_focus(field, defaultString) {
	// Default behavior
}

function field_blur(field, defaultString) {
	// Default behavior
}

function changePassword(){
    var currentusername = usernameField[0].value;
    var currentpassword = currentPasswordField[0].value;
	var newPassword = newPasswordField[0].value;

	console.log(currentusername);
	console.log(currentpassword);
    console.log(newPassword)

	// TODO: Login to server here
	console.log("Password Reset!");
}

$(document).ready( () => {
	// initailize Field variables
	usernameField = $("#usernameInput");
	newPasswordField = $("#newPasswordInput");
    currentPasswordField = $("#currentPasswordInput");

	// Add event listeners to the input fields

	var enterPressed = "enterPressed"

	// On enter while in the username field, change focus to the code field
	usernameField.on(enterPressed, event => currentPasswordField.focus());

    // On enter while in the code field, change focus to the new password field
	currentPasswordField.on(enterPressed, event => newPasswordField.focus());

    // On enter while in the newpassword field, chang password
	newPasswordField.on(enterPressed, changePassword);

	// Establish a 'Enter' key listener
	$("input").on("keyup", function (event) {
		// if the 'Enter' key is pressed
		if (event.which == 13) {
			$(this).trigger("enterPressed");
		}
	});







});