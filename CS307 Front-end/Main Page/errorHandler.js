function genericErrorHandlers(error) {
	if (error == "NotLoggedInToService: User needs to log in to streaming service") {
		grantServerAccessRedirect();
		return;
	}
	
	if (error == "Unauthenticated: User needs to log in to interLinked") {
		logoutRedirect();
		return;
	}
}