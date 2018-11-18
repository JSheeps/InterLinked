"use strict";
var passwordField;
var passwordConfirmField;
var resetToken;

function resetPassword() {
	var password = passwordField.val();
	var passwordConfirm = passwordConfirmField.val();
	
	if (password != passwordConfirm) {
		alert("Passwords don't match");
		return;
	}
	
	if (password.length == 0) {
		alert("Need to input a password");
		return;
	}
	
	console.log(password);
	serverResetPassword(resetToken, password).done( (result) => {
		console.log(result);
		if (result.error) {
			alert(result.error);
		} else {
		}
	});
}

$(document).ready( () => {
	passwordField = $("#passwordInput");
	passwordConfirmField = $("#passwordConfirmInput");
	
	$("input").on("keyup", function(event) {
		if (event.which == 13)
			$(this).trigger("enterPressed");
	});
	
	passwordField.on("enterPressed", () => passwordConfirmField.focus());
	passwordConfirmField.on("enterPressed", resetPassword);
	
	
	resetToken = new URLSearchParams(window.location.search).get("resetToken");
});