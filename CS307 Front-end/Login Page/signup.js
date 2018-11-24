"use strict";

// Global variables to reduce Querying

var userNameField;
var passwordField;
var emailField

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

function getEmail() {
	return emailField[0].value;
}

function login() {
	logoutRedirect();
}

function validEmail(email) { 
	var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(String(email).toLowerCase());
}

function signUp() {
	var userName = getUserName();
	var password = getPassword();
	var email =  getEmail();
	
	if (userName.length == 0) {
		alert("Need to input a username");
		return;
	}
	
	if (!validEmail(email)) {
		alert("Invalid email");
		return;
	}
	
	if (password.length == 0) {
		alert("Need to input a password");
		return;
	}
	
	var buttons = $("div.btnRef");
	var buttonTexts = [];
	var buttonClicks = [];
	for (var i = 0; i < buttons.length; i++) {
		var button = buttons[i];
		buttonTexts.push(button.innerHTML);
		button.innerHTML = "Loading...";
		buttonClicks.push(button.onclick);
		button.onclick = null;
	}
	
	$("input").prop("readonly", true);
	
	serverSignup(userName, password, email).done( (result) => {
		if (!result.result) {
			if (result.error)
				alert(result.error);
			else				
				alert("Invalid signup details");
		} else {
			serverLogin(userName, password).done( (result) => {
				if (!result.result) {
					if (result.error)
						alert(result.error);
					else
						alert("Error: Invalid login");
				} else {
					var token = result.authenticate;
					setAuthData(token, userName);
					playlistRedirect();
				}
			});
		}
		
		$("input").prop("readonly", false);
		for (var i = 0; i < buttons.length; i++) {
			var button = buttons[i];
			button.innerHTML = buttonTexts[i];
			button.onclick = buttonClicks[i];
		}
	});
}

$(document).ready( () => {	
	// initailize Field variables
	userNameField = $("#userNameInput");
	passwordField = $("#passwordInput");
	emailField = $("#emailInput");
	
	// Add event listeners to the input fields
	
	var enterPressed = "enterPressed"
	// On enter while in the password field, signup
	passwordField.on(enterPressed, signUp);
	
	// On enter while in the username field, change focus to the email field
	userNameField.on(enterPressed, (event) => {
		if (getUserName().length != 0)
			emailField.focus();
		else
			userNameError("Need to input a username");
	});
	
	// On enter while in the email field, change focus to the email field
	emailField.on(enterPressed, (event) => {
		if (getEmail().length != 0)
			passwordField.focus();
		else
			emailError("Need to input an email");
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