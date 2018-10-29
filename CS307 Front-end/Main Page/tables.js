"use strict"

var pageContents;

$(document).ready( () => {
	pageContents = $("#mainContents");
});

function clearTable(tableSelector) {
	var table = pageContents.find(tableSelector);
	table.find("tr:gt(0)").remove();
	return pageContents.find(tableSelector + " tr:last");
}