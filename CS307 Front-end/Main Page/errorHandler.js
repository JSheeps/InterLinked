function genericErrorHandlers(error) {
	if (error == "Not logged in: User needs to log in to a streaming service") {
		grantServerAccessRedirect();
		return;
	}
	
	if (error == "Unauthenticated: User needs to log in to interLinked") {
		logoutRedirect();
		return;
	}
}