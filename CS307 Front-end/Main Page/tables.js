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
		this.tbody.html("<tr> <th colspan='" + this.columns + "'>" + (title ? title : "") + "</th></tr>");
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
		sortData: %sort data, defaults to val%
		span: %number, how many columns the element should span (defaults to 1)%
		attr: {%attributes to assign to the <td>%
			e.g: "class": "clickable"
		}
	}
	only val is required
	*/
	addRow(rowAttribs, ...objs) {
		this.addRowAfter("tr:last", rowAttribs, ...objs);
	}
	
	addRowAfter(selector, rowAttribs, ...objs) {
		if (typeOf(objs[0]) == "array")
			objs = objs[0];
		
		var span = 0;
		for (var i = 0; i < objs.length; i++) {
			var obj = objs[i];
			span += (obj.span ? obj.span : 1);
		}
			
		if (span != this.columns)
			throw "number of objects != " + this.columns;

		
		var lastRow = this.tbody.find(selector);
		var header = serialize(rowAttribs);
		var html = "<tr" + header + ">";
		
		for (var i = 0; i < objs.length; i++) {
			var obj = objs[i];
			var objType = typeOf(obj);
			html += "<td";
			if (this.sortColumnsTitles != null && this.sortColumnsTitles[i] != null && typeOf) {
				var sortData = getSortData(obj);
				if (sortData != "")
					html += " sortData='" + getSortData(obj) + "'";
			}
			
			if (obj.span)
				html += " colspan='" + obj.span + "'";
			
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
	
	removeRow(i) {
		getRow(i).clear();
	}
	
	getRow(index) {
		return this.tbody.children().eq(index);
	}
	
	
	getRowByID(id) {
		return this.tbody.find("#" + id);
	}
	
	hide() {
		this.table.hide();
	}
	
	show() {
		this.table.show();
	}
	
	rows() {
		return this.tbody.children().length;
	}
	
	makeSortRow(tableName = "table") {
		var row =[];
		for (var i = 0; i < this.sortColumnsTitles.length; i++) {
			var title = this.sortColumnsTitles[i];
			var titleEntry;
			if (title == null) {
				titleEntry = "";
			} else {
				var titleEntry = { val: "<u>" + this.sortColumnsTitles[i] + "</u>" };
				titleEntry.attr = { 
					onclick: "'" + tableName + ".sort(" + i + ")'",
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
	
	remove(selectorOrIndex) {
		var type = typeOf(selectorOrIndex);
		console.log(type);
	}
	
	sort(column) {
		if (this.sortColumnsTitles == null)
			return;
		
		var ascending = this.ascending(column);
		
		
		var sortFunc = (a, b) => {
			return compare(a, b, ascending, column);	
		};
		
		var rows = this.tbody.children("tr:gt(1)");//.sort(sortFunc).appendTo(this.tbody);
		if (rows.length < 2)
			return;
		
		if (binding(rows[0]) == -1)
			throw "First row is bound to row above it";
		
		rollup(rows);
		
		rows.sort(sortFunc)
		
		rows = unroll(rows);
		rows.appendTo(this.tbody);
		
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

function rollup(rows) {
	for (var i = rows.length - 1; i > 0; i--) {
		var row = rows[i];
		var bind = binding(row);
		if (bind) {
			if (bind == -1) {
				var boundRow = rows.splice(i, 1)[0];
				rows[i - 1].boundRow = boundRow;
			} else {
				throw "unsupported bind";
			}
		}
	}
}

function unroll(rows) {
	var ret = [];
	for (var i = 0; i < rows.length; i++) {
		var row = rows[i];
		ret.push(row);
		while (row.boundRow) {
			row = row.boundRow;
			ret.push(row);
		}
	}
	
	return $(ret);
}

function binding(row) {
	var bind = $(row).attr("bind");
	if (bind == "UP")
		return -1;
	
	return 0;
}

function compare(a, b, ascending, column) {
	var asd = $(a).children().eq(column).attr("sortData");
	var bsd = $(b).children().eq(column).attr("sortData");
	
	if (asd == bsd)
		return 0;
	
	if (asd < bsd) 
		return ascending ? -1 : 1;
	
	return ascending ? 1 : -1;
}

function getSortData(obj) {
	if (obj.sortData)
		return obj.sortData;
	
	var type = typeOf(obj);
	if (type == "string")
		return obj;
	
	return undefined;
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