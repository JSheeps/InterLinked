// Page redirects
function logoutRedirect() {
	eraseCookie("auth");
	redirect("../index.html");
}

function loginRedirect() {
	redirect("../Main Page/viewPlaylists.html");
}

function playlistRedirect() {
	redirect("../Main Page/viewPlaylists.html");
}

function importRedirect() {
	redirect("../Main Page/import.html");
}

function exportRedirect() {
	redirect("../Main Page/export.html");
}

function mergeRedirect() {
	redirect("../Main Page/merge.html");
}

function redirect(URI) {
	window.location.href = URI;
}

// Cookies
function createCookie(name, value = "", expiration = -1, path = "/") {
	console.log(expiration);
	var expires = (expiration === -1) ?
		"" :
		"; expires=" + expiration.toGMTString();
	var cookieString = name + "=" + value + expires + "; path=" + path;
	console.log(cookieString.toString());
	document.cookie = cookieString;
	console.log(document.cookie);
}

function readCookie(name) {
	console.log(document.cookie);
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for (var i=0; i < ca.length; i++) {
		var c = ca[i];
		while (c.charAt(0)==' ') c = c.substring(1, c.length);
		if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
	}
	return null;
}

function eraseCookie(name) {
	createCookie(name, "", new Date(0), "/");
}