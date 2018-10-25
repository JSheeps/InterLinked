"use strict";

function sendToSpotify(){
    console.log("Sent user to Spotify!");
}

$(document).ready( () => {
	SpotifyInit();
});

function SpotifyInit() {
	var SpotifyLink = $("#SpotifyLink");
	var SpotifyUrl = SpotifyLink.attr("href");
	var redirectURI = "https%3A%2F%2Fwww.google.com%2F&scope=user-read-birthdate%2Cuser-read-email&show_dialog=true";
	console.log(decodeURIComponent(redirectURI));

	// Note from Andrew: added login endpoint
	var redirectURI = encodeURIComponent("http://localhost/login/" /*url*/); // ?authenticate=" + getAuthToken() + "&platformInfo=Spotify");
	console.log(decodeURIComponent(redirectURI));
	SpotifyUrl += redirectURI;

	// Note from Andrew: added state variable
	SpotifyUrl += "&state=";
	SpotifyUrl += getAuthToken();
	console.log(SpotifyUrl);

	SpotifyLink.attr("href", SpotifyUrl);
}