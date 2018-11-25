"use strict";

// Global variables to reduce Querying
var usernameField;
var newPasswordField;
var currentPasswordField;
var confirmPasswordField;

function changePassword(){
    var currentusername = usernameField.val();
    var currentpassword = currentPasswordField.val();
	var newPassword = newPasswordField.val();
	var confirmPassword = confirmPasswordField.val();
	
	if (currentusername.length == 0) {
		alert("Need to input a username");
		return;
	}
	
	if (currentpassword.length == 0) {
		alert("Need to input the login password");
		return;
	}
	
	if (newPassword.length == 0) {
		alert("Need to input a new password");
		return;
	}
	
	if (confirmPassword.length == 0) {
		alert("Need to confirm the new password");
		return;
	}
	
	if (confirmPassword != newPassword) {
		alert("Error: the new password does not match the confirming password");
		return;
	}

	serverChangePassword(currentusername, currentpassword, newPassword).done( (result) => {
		if (result.error) {
			alert(result.error);
			return;
		} else {
			if (result.result)
				alert("Password Successfully Changed");
			else
				alert("Password could not be changed");
		}
	});
}

$(document).ready( () => {
	// initailize Field variables
	usernameField = $("#usernameInput");
    currentPasswordField = $("#currentPasswordInput");
	newPasswordField = $("#newPasswordInput");
	confirmPasswordField = $("#confirmPasswordInput");

	// Add event listeners to the input fields

	var enterPressed = "enterPressed"

	// On enter while in the username field, change focus to the code field
	usernameField.on(enterPressed, event => currentPasswordField.focus());

    // On enter while in the code field, change focus to the new password field
	currentPasswordField.on(enterPressed, event => newPasswordField.focus());

    // On enter while in the newpassword field, chang password
	newPasswordField.on(enterPressed, event => confirmPasswordField.focus());
	
	confirmPasswordField.on(enterPressed, changePassword);

	// Establish a 'Enter' key listener
	$("input").on("keyup", function (event) {
		// if the 'Enter' key is pressed
		if (event.which == 13) {
			$(this).trigger(enterPressed);
		}
	});
});