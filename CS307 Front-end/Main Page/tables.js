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

class Table {
	constructor(id, title, ...sortColumnsTitles) {
		this.id = id;
		this.table = $(id);
		this.tbody = this.table.children();
		if (this.tbody.length == 0) {
			this.table.append("<tbody></tbody>");
			this.tbody = this.table.children();
		}
		this.columns = sortColumnsTitles.length;
		
		this.sortColumnsTitles = sortColumnsTitles;
		this.orderBy = -1;
		
		this.tbody.empty();
		this.tbody.html("<tr> <th colspan='" + this.columns + "'>" + title + "</th></tr>");
	}
	
	/*
	1st argument: an (array of) attributes to put into the row entry.
	e.g.:
		{ class: "row1" }
	e.g.:
	[
		{
			class: "row1",
			order: "up"
		},
		{ class: "hi" }
	]
	
	2nd argument:
	takes either dynamic params or one array
	Each element is either a string or an object like so:
	{
		val: %value e.g. string%
		attr: {%attributes to assign to the <td>%
			e.g: "class": "clickable"
		}
	}
	only val is required
	*/
	addRow(rowAttribs, ...objs) {
		if (typeOf(objs[0]) == "array")
			objs = objs[0];
		if (objs.length != this.columns)
			throw "number of objects != " + this.columns;

		
		var lastRow = this.tbody.find("tr:last");
		var header = serialize(rowAttribs);
		var html = "<tr" + header + ">";
		
		for (var i = 0; i < objs.length; i++) {
			var obj = objs[i];
			var objType = typeOf(obj);
			html += "<td";
			if (this.sortColumnsTitles != null && this.sortColumnsTitles[i] != null)
				html += " sortData=" + getSortData(obj);
			
			if (objType == "string") {
				html +=">" + obj + "</td>";
			} else if (objType == "object") {
				for (var attrib in obj.attr)
					html += " " + attrib + "=" + obj.attr[attrib];
				html += ">" + obj.val + "</td>";
			}
		}
		html +="</tr>";
		
		lastRow.after(html);
	}
	
	getRow(index) {
		return this.tbody.children().eq(index);
	}
	
	rows() {
		return this.tbody.children().length;
	}
	
	makeSortRow() {
		var row =[];
		for (var i = 0; i < this.sortColumnsTitles.length; i++) {
			var title = this.sortColumnsTitles[i];
			var titleEntry;
			if (title == null) {
				titleEntry = "";
			} else {
				var titleEntry = { val: "<u>" + this.sortColumnsTitles[i] + "</u>" };
				titleEntry.attr = { 
					onclick: "'table.sort(" + i + ")'",
					"class": "clickable" 
				};
			}
			row.push(titleEntry);
		}
		
		this.addRow(null, row);
	}
	
	clear() {
		this.table.find("tr:gt(0)").remove();
		return this;
	}
	
	sort(column) {
		if (this.sortColumnsTitles == null)
			return;
		
		var ascending = this.ascending(column);
		
		
		var sortFunc = (a, b) => {
			return compare(
				$(a).children().eq(column).attr("sortData"),
				$(b).children().eq(column).attr("sortData"),
				ascending
			);	
		};
		
		this.tbody.find("tr:gt(1)").sort(sortFunc).appendTo(this.tbody);
		this.tbody.attr("order", (ascending ? "asc" : "desc"));
		return this;
	}
	
	ascending(column) {
		if (this.orderBy == column) {
			var asc = this.tbody.attr("order");
			if (asc == undefined || asc == "desc")
				return true;
			else if (asc == "asc")
				return false;
			else throw "Neither ascending nor descending";
		} else {
			this.orderBy = column;
			return true;
		}
	}
	
	text(txt, column = 0, tag = "i") {
		if (column < 0 || column > this.columns)
			throw "column out of range";
		
		this.clear();
		var arr =[];
		var openTag = (tag ? "<" + tag + ">" : "");
		var closeTag = (tag ? "</" + tag + ">" : "");
		for (var i = 0; i < this.columns; i++)
			arr.push(i == column ? (openTag + txt + closeTag) : "");
		
		this.addRow(null, arr);
		
		return this;
	}
	
	loading() {
		this.clear();
		this.text("Loading...");
	}
}

function compare(a, b, ascending) {
	if (a == b) return 0;
	if (a < b) {
		return ascending ? -1 : 1;
	}
	return ascending ? 1 : -1;
}

function getSortData(obj) {
	return obj;
}

function typeOf( obj ) {
  return ({}).toString.call( obj ).match(/\s(\w+)/)[1].toLowerCase();
}

function serialize(obj) {
	if (!obj) return "";
	
	var ret = "";
	var objType = typeOf(obj);
	
	if (objType == "array") {
		for (var i = 0; i < obj.length; i++)
			ret += serialize(obj[i]);
	} else if (objType == "object") {
		for (var attrib in obj) {
			ret += " " + attrib + "=" + obj[attrib];
		}
	} else throw objType;
	return ret;
}