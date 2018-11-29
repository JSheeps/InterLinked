"use strict";
const domain = window.location.origin;

function sendToSpotify(){
    console.log("Sent user to Spotify!");
}

$(document).ready( () => {
	SpotifyInit();
});

function SpotifyInit() {
    var SpotifyLink = $("#SpotifyLink");
	var SpotifyUrl = SpotifyLink.attr("href");

	var redirectURI = domain + "/login/?platformID=Spotify";
	
	SpotifyUrl += encodeURIComponent(redirectURI);

	SpotifyUrl += "&state=";
	SpotifyUrl += getAuthToken();

	SpotifyLink.attr("href", SpotifyUrl);
}

function sendToYoutube(){
    console.log("Sent user to Youtube!");
}

$(document).ready( () => {
    YoutubeInit();
});

function YoutubeInit() {
    var YoutubeLink = $("#YoutubeLink");
    var YoutubeUrl = YoutubeLink.attr("href");

    var redirectURI = domain + "/login/?platformID=Youtube";
    YoutubeUrl += encodeURIComponent(redirectURI);

    YoutubeUrl += "&state=";
    YoutubeUrl += getAuthToken();

    YoutubeLink.attr("href", YoutubeUrl);
}

function getGoogleInfo(){
	var connectImage = $(".GoogleConnect").parent();
	connectImage.empty();
	
	connectImage.append(makeInputField("GoogleUsernameInput", "username", { placeholder: "Google Music Username"} ));
	connectImage.append(makeInputField("GooglePasswordInput", "password", { placeholder: "Google Music Password"} ));
	
	var input = makeInputField("GoogleIMEIinput", "text", { placeholder: "IMEI from mobile device" } );
	input.on("enterPressed", sendGoogleInfo);
	connectImage.append(input);
	connectImage.append("<br>");
	connectImage.append(makeInputField("GoogleSubmit", "submit", { onclick: "sendGoogleInfo()" } ));
}

function makeInputField(id, type, other = null, internal = "") {
	return $("<input>" + internal + "</input>").attr({
		id: id,
		type: type,
	}).attr(other)
	.on("keyup", function (event) {
		if (event.which == 13)
			$(this).trigger("enterPressed");
	});
}

var authorizing = false;
function sendGoogleInfo() {
	if (authorizing)
		return;
	
	var username = $("input#GoogleUsernameInput").val();
	if (username.length == 0) {
		alert("Need to input a username");
		return;
	}
	
	var password = $("input#GooglePasswordInput").val();
	if (password.length == 0) {
		alert("Need to input password");
		return;
	}
	
	var imei = $("input#GoogleIMEIinput").val();
	if (imei == 0) {
		alert("Need to input IMEI of an android device that had GooglePlayMusic installed");
		return;
	}
	
	authorizing = true;
	serverSendGoogleInfo(username, password, imei).done( (result) => {
		if (result.error) {
			alert(result.error);
		} else {
			if (result.result)
				alert("Success!");
			else {
				alert("Bug found. Check console")
				console.log(result.error);
			}
		}
		authorizing = false;
	});
}