"use strict";

// Global variables to reduce Querying
var usernameField;


function requestCode() {
	var username = usernameField.val();
	if (username.length == 0) {
		alert("Must input a username");
		return;
	}
	
	var button = $("#codebtn");
	button.prop("disabled");
    
	serverForgetPassword(username).done(result => {
		if (result.error) {
			alert(result.error);
		} else {
			alert("Email to reset the password is at " + result.email);
		}
	});
}

$(document).ready( () => {
	// initailize Field variables
	usernameField = $("#usernameInput");
	
	// Add event listeners to the input fields
	var enterPressed = "enterPressed";
	usernameField.on(enterPressed, requestCode);
    
	// Establish a 'Enter' key listener	
	$("input").on("keyup", function (event) {
		// if the 'Enter' key is pressed
		if (event.which == 13) {
			$(this).trigger("enterPressed");
		}
	});
});