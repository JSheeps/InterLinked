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
	console.log(SpotifyUrl);

	var redirectURI = "http://localhost/login/?platformID=Spotify";
	console.log(redirectURI);
	
	SpotifyUrl += encodeURIComponent(redirectURI);

	SpotifyUrl += "&state=";
	SpotifyUrl += getAuthToken();
	console.log(SpotifyUrl);

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
    console.log(YoutubeUrl);

    var redirectURI = "http://localhost/login/?platformID=Youtube";
    console.log(redirectURI);

    YoutubeUrl += "&state=";
    YoutubeUrl += getAuthToken();
    console.log(YoutubeUrl);

    YoutubeLink.attr("href", YoutubeUrl);
}

function getGoogleInfo(){
    var username = prompt("Please enter your GooglePlayMusic Username", "Enter username here");
    var password = prompt("Please enter your GooglePlayMusic Password", "Enter password here");
    var androidId = prompt("Please enter the IMEI of an android device that had GooglePlayMusic installed", "Enter IMEI here");

    console.log(username);
    console.log(password);
    console.log(androidId);
}