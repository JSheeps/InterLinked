"use strict";

// Global variables to reduce Querying
var emailField;
var newPasswordField;
var codeField;

function field_focus(field, defaultString) {
	// Default behavior
}

function field_blur(field, defaultString) {
	// Default behavior
}

function sendCode() {
    var email=emailField[0].value;
    
    console.log("Sent code to: "+email);
}

function changePassword(){
    var email = emailField[0].value;
    var code = codeField[0].value;
	var newPassword = newPasswordField[0].value;

	console.log(email);
	console.log(code);
    console.log(newPassword)
	
	// TODO: Login to server here
	console.log("Password Reset!");
}

$(document).ready( () => {
	// initailize Field variables
	emailField = $("#emailInput");
	newPasswordField = $("#newPasswordInput");
    codeField = $("#codeInput");
	
	// Add event listeners to the input fields
	
	var enterPressed = "enterPressed"
	
	// On enter while in the username field, change focus to the code field
	emailField.on(enterPressed, event => codeField.focus());
    
    // On enter while in the code field, change focus to the new password field
	codeField.on(enterPressed, event => newPasswordField.focus());
    
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