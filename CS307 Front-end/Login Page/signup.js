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
	
	serverSignup(userName, password, email).done( (accessToken) => {
		if (!accessToken.result) {
			alert("Invalid signup details");
		} else {
			console.log(accessToken.accessToken);
			createCookie("accessToken", accessToken.accessToken);
			playlistRedirect();
			
			//used for displaying usernames on each page
			sessionStorage.setItem("username", userName);
			
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
	});
}

$(document).ready( () => {
	// check if already has session token
	document.cookie = "";
	var token = readCookie("accessToken");
	if (token)
		playlistRedirect();
	
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