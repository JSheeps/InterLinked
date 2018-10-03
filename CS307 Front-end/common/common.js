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
function createCookie(name, value = "", expiration = new Date().getTime() + (60 * 60 * 1000), path = "/") {
	var expires = "; expires=" + expiration.toGMTString();
	document.cookie = name+"="+value+expires+"; path=" + path;
}

function readCookie(name) {
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