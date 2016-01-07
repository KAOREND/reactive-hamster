hamster.main.getJavascriptName = function(cssname) {
    if (cssname == "background-attachment") {
	return "backgroundAttachment";
    } else if (cssname == "background-color") {
	return "backgroundColor";
    } else if (cssname == "background-image") {
	return "backgroundImage";
    } else if (cssname == "background-position") {
	return "backgroundPosition";
    } else if (cssname == "background-repeat") {
	return "backgroundRepeat";
    } else if (cssname == "border-bottom") {
	return "borderBottom";
    } else if (cssname == "border-bottom-color") {
	return "borderBottomColor";
    } else if (cssname == "border-bottom-style") {
	return "borderBottomStyle";
    } else if (cssname == "border-bottom-width") {
	return "borderBottomWidth";
    } else if (cssname == "border-color") {
	return "borderColor";
    } else if (cssname == "border-left") {
	return "borderLeft";
    } else if (cssname == "border-left-color") {
	return "borderLeftColor";
    } else if (cssname == "border-left-style") {
	return "borderLeftStyle";
    } else if (cssname == "border-left-width") {
	return "borderLeftWidth";
    } else if (cssname == "border-right") {
	return "borderRight";
    } else if (cssname == "border-right-color") {
	return "borderRightColor";
    } else if (cssname == "border-right-style") {
	return "borderRightStyle";
    } else if (cssname == "border-right-width") {
	return "borderRightWidth";
    } else if (cssname == "border-style") {
	return "borderStyle";
    } else if (cssname == "border-top") {
	return "borderTop";
    } else if (cssname == "border-top-color") {
	return "borderTopColor";
    } else if (cssname == "border-top-style") {
	return "borderTopStyle";
    } else if (cssname == "border-top-width") {
	return "borderTopWidth";
    } else if (cssname == "border-width") {
	return "borderWidth";
    } else if (cssname == "caption-side") {
	return "captionSide";
    } else if (cssname == "empty-cells") {
	return "emptyCells";
    } else if (cssname == "css-float") {
	return "cssFloat";
    } else if (cssname == "font-family") {
	return "fontFamily";
    } else if (cssname == "font-size") {
	return "fontSize";
    } else if (cssname == "font-stretch") {
	return "fontStretch";
    } else if (cssname == "font-style") {
	return "fontStyle";
    } else if (cssname == "font-variant") {
	return "fontVariant";
    } else if (cssname == "font-weight") {
	return "fontWeight";
    } else if (cssname == "letter-spacing") {
	return "letterSpacing";
    } else if (cssname == "line-height") {
	return "lineHeight";
    } else if (cssname == "list-style") {
	return "listStyle";
    } else if (cssname == "lsit-style-image") {
	return "listStyleImage";
    } else if (cssname == "lsit-style-position") {
	return "listStylePosition";
    } else if (cssname == "list-style-type") {
	return "listStyleType";
    } else if (cssname == "margin-bottom") {
	return "marginBottom";
    } else if (cssname == "margin-left") {
	return "marginLeft";
    } else if (cssname == "margin-right") {
	return "marginRight";
    } else if (cssname == "margin-top") {
	return "marginTop";
    } else if (cssname == "max-height") {
	return "maxHeight";
    } else if (cssname == "max-width") {
	return "maxWidth";
    } else if (cssname == "min-height") {
	return "minHeight";
    } else if (cssname == "min-width") {
	return "minWidth";
    } else if (cssname == "padding-bottom") {
	return "paddingBottom";
    } else if (cssname == "padding-left") {
	return "paddingLeft";
    } else if (cssname == "padding-right") {
	return "paddingRight";
    } else if (cssname == "padding-top") {
	return "paddingTop";
    } else if (cssname == "page-break-after") {
	return "pageBreakAfter";
    } else if (cssname == "page-break-before") {
	return "pageBreakBefore";
    } else if (cssname == "scrollbar-3dlight-color") {
	return "scrollbar3dLightColor";
    } else if (cssname == "scrollbar-arrow-color") {
	return "scrollbarArrowColor";
    } else if (cssname == "scrollbar-base-color") {
	return "scrollbarBaseColor";
    } else if (cssname == "scrollbar-darkshadow-color") {
	return "scrollbarDarkshadowColor";
    } else if (cssname == "scrollbar-face-color") {
	return "scrollbarFaceColor";
    } else if (cssname == "scrollbar-highlight-color") {
	return "scrollbarHighlightColor";
    } else if (cssname == "scrollbar-shadow-color") {
	return "scrollbarShadowColor";
    } else if (cssname == "scrollbar-track-color") {
	return "scrollbarTrackColor";
    } else if (cssname == "table-layout") {
	return "tableLayout";
    } else if (cssname == "text-align") {
	return "textAlign";
    } else if (cssname == "text-decoration") {
	return "textDecoration";
    } else if (cssname == "text-indent") {
	return "textIndent";
    } else if (cssname == "text-transform") {
	return "textTransform";
    } else if (cssname == "vertical-align") {
	return "verticalAlign";
    } else if(cssname == "visibility") {
	return "visible";
    } else if (cssname == "word-spacing") {
	return "wordSpacing";
    } else if (cssname == "z-index") {
	return "zIndex";
    } else {
	return cssname;
    }
}

hamster.main.applyStyle = function(obj, styleString) {
    var attrArray = styleString.split(";");
    for (i = 0; i<attrArray.length; i++) {
	var parts = attrArray[i].split(":");
	var name = hamster.main.getJavascriptName(hamster.main.trim(parts[0]));
	if (name == "") {
	    return;
	}
	var value = hamster.main.trim(parts[1]);
	var evalstring = "obj.style." + name + " = \"" + value + "\"";
	//alert("eval: " + evalstring);
	try {
	    eval(evalstring);
                        
	} catch(e) {
	    alert("applystyles: exception: "+e+" "+stylsString);
	}
    }
}

hamster.main.trim = function(str) {
    if (!str) {
	return "";
    }
    str = str.replace(/^\s+/, ''); // remove leading whitespace
    str = str.replace(/\s+$/, ''); // remove trailing whitespace
    str = str.replace(/\s+/g, ' '); // remove double whitespace
    return str;
}