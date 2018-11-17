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