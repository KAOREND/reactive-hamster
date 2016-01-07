
/*
 Variables from configuration:
 hamster.Config.URL_h_request;
 hamster.Config.URL_refresh;
 hamster.Config.contextRoot;
 */

//Definieren des Namespaces fuer die FrontEnd-Hauptfunktionalitaet
hamster.main = {};


hamster.main.currentPath = ""; //current static url
hamster.main.http_request = false;
hamster.main.isRunningRequest = false;
hamster.main.id_count = 0;
hamster.main.STATUSBAR_Y = 0;
hamster.main.PAGELOADER_IMG;
hamster.main.current = 0;
//Liste möglicher Eventtypen
hamster.main.EVENTS = new Array();
hamster.main.EVENTS[0] = "onclick";
//hamster.main.EVENTS[1] = "onmouseover";
//hamster.main.EVENTS[2] = "onmousedown";
//hamster.main.EVENTS[3] = "onmouseup";
//usf... bei Bedarf ergaenzen

hamster.main.Cache = new Array();
hamster.main.queue = new Array();
hamster.main.websocket_pending_queue = new Array();
hamster.main.queue_count = 0;
hamster.main.worker_count = 0;
hamster.main.error = false;
//waitcount gibt an, bis zu welchem work_count arbeitstatus angezeigt wird
hamster.main.wait_count = -1;
hamster.main.refreshTime = 100;
hamster.main.waiting = false;
hamster.main.silenRequest = false;
hamster.main.makingRefresh = false;
hamster.main.pageUpdateRunning = false;
hamster.main.retries = 0;
hamster.main.debug = false;
hamster.main.onkeyTimeout;
hamster.main.loaderTimeout;
hamster.main.iframes = new Array();
hamster.main.websocket;
hamster.main.websocketFailed=false;
hamster.main.active = true;
hamster.main.focusStealingBlocked = false;

hamster.main.initialize= function () {
    hamster.main.initHTMLNodes(window.document);
    hamster.main.initWebSocket();

    window.setTimeout("hamster.main.historyLoop()", 250);

    if (hamster.main.websocket == null) {
        window.setTimeout("hamster.main.worker()", 100);
    }
    window.setTimeout("hamster.main.refreshLoop2()", 2000);
    document.onmousemove = hamster.main.setActive;
    document.onmousedown = hamster.main.setActive;
    document.onkeypress = hamster.main.setActive;
    document.onscroll = hamster.main.setActive;
}

/**
 Initialisierung des DOM
 */
hamster.main.initHTMLNodes = function (xmlNode) {
    try {
        if (xmlNode.nodeType != Node.TEXT_NODE) {
            for (var i = 0; i < xmlNode.childNodes.length; i++) {
                hamster.main.initHTMLNodes(xmlNode.childNodes[i]);
            }

            try {
//add HTML Node to Cache:
                if (xmlNode.getAttribute && xmlNode.getAttribute("id") && xmlNode.getAttribute("id").charAt(0) != "L") {
                    hamster.main.Cache[xmlNode.getAttribute("id")] = xmlNode;
                }
            } catch (e) {
                hamster.main.log(e);
                if (hamster.main.debug) {
                    alert("initHMTLNodes(): " + xmlNode.nodeName + " \n " + e);
                }
            }
        }
    } catch (e) {
        hamster.main.log(e);
        alert("initHMTLNodes(): " + xmlNode.nodeName + " \n " + e);
    }
}

hamster.main.initWebSocketLoopStarted = false;
hamster.main.initWebSocket = function () {
    try {
        if (hamster.main.initWebSocketLoopStarted) {
            return;
        }
        hamster.main.initWebSocketLoopStarted = true;
        var l = window.location;
        
        var wsURL = ((l.protocol === "https:") ? "wss://" : "ws://") + l.hostname + (((l.port != 80) && (l.port != 443) && (l.port != "") && (l.port != null)) ? ":" + l.port : "") +  hamster.Config.contextRoot +"websocket";

        hamster.main.log("trying to connect a WebSocket to " + wsURL);
        var ws = new WebSocket(wsURL);
        hamster.main.websocket = ws;
        hamster.main.websocket.onopen = function () {
            hamster.main.initWebSocketLoopStarted = false;
            hamster.main.log("WebSocket opened");
            hamster.main.refreshTime = 10000; //with websocket we only need to notify that we are still alive
            if (hamster.main.websocket_pending_queue.length > 0) {
                hamster.main.log("WebSocket opened: do pending queue entries");
                for (var i = 0; i < hamster.main.websocket_pending_queue.length; i++) {
                    var message = hamster.main.websocket_pending_queue[i];
                    try {
                        hamster.main.log("WebSocket opened: do pending queue entries send:" + message);
                        hamster.main.websocket.send(message);
                    } catch (e) {
                        hamster.main.log("WebSocket opened: do pending queue entries error: :" + e);
                    }
                }
                hamster.main.websocket_pending_queue = new Array();
            }
        };

        var domParser = new DOMParser()
        hamster.main.websocket.onmessage = function (message) {
            try {
                if (message) {
                    hamster.main.log("receiving message" + message.data);
                    if (message.data.lastIndexOf('failed', 0) == 0) {
                        hamster.main.log("error from server, reloading page: " + message.data);
                        window.location.reload();
                    } else {
                        //parse to XML
                        var xml = domParser.parseFromString(message.data, "text/xml");
                        hamster.main.applyXML(xml);
                        hamster.main.isRunningRequest = false;
                        hamster.main.makingRefresh = false;
                        document.body.style.cursor = "";
                        if (hamster.deactivateLoader) {
                            if (hamster.main.loaderTimeout) {
                                window.clearTimeout(hamster.main.loaderTimeout);
                            }
                            hamster.deactivateLoader();
                        }
                        hamster.main.log("processed message");
                    }
                }
            } catch (e) {
                hamster.main.log("error while processing websocket message: " + e);
            }
        };


        hamster.main.websocket.onclose = function () {
            hamster.main.initWebSocketLoopStarted = false;
            if( hamster.main.onWebSocketClose) {
                hamster.main.onWebSocketClose();
            }
            
            hamster.main.log("websocket connection closed, trying to reconnect again ");
            hamster.main.websocket=null;
            setTimeout(function () {
                hamster.main.initWebSocket()
            }, 2000);
        }

        hamster.main.websocket.onerror = function (event) {
            hamster.main.log("websocket onerror " + event);
        }

    } catch (e) {
        hamster.main.websocketFailed=true;
        hamster.main.websocket = null;
        hamster.main.log("could not intialize a websocket connection " + e);
    }
}




/**
 Diese Funktion prueft in regelmaessigen Abstaenden, ob der Vor oder Zurueck Button
 betaetigt wurde. Falls ja, wird ein entsprechender History-Request an den Server gesendet
 */
hamster.main.historyLoop = function () {
    hamster.main.checkHashURL();
    if(window.onhashchange) {
         window.onhashchange = hamster.main.checkHashURL();
    } else {
        window.setTimeout("hamster.main.historyLoop()", 100);
    }
}

hamster.main.hasHashURLChanged = function () {
     if (window.location.hash) {
        hamster.main.current = window.location.hash;
        hamster.main.current = hamster.main.current.replace(/#/, '');
        if (hamster.main.current != "" && hamster.main.current != hamster.main.currentPath) {
           return true;
        }
    } 
    return false;
}

hamster.main.checkHashURL = function () {
    h_request = hamster.Config.URL_h_request;
    if (window.location.hash) {
        hamster.main.current = window.location.hash;
        hamster.main.current = hamster.main.current.replace(/#/, '');
        if (hamster.main.current != "" && hamster.main.current != hamster.main.currentPath) {
            hamster.main.currentPath = hamster.main.current;
            hamster.main.openStaticURL(hamster.main.currentPath);
        }
    }
}

hamster.main.openStaticURL = function (staticURL) {
    hamster.main.doRequestAuxiliary(hamster.Config.URL_static + "?" + staticURL);
}

hamster.main.updateHashURL = function (newHash) {
//check if there is an old unprocessed hash:
    var old = hamster.main.currentPath;
    hamster.main.checkHashURL();
    if (old != hamster.main.currentPath) {
// looks like the old hash had to porcessed first, ignore the new one
    } else {
        window.location.hash = newHash;
        hamster.main.currentPath = newHash;
    }
}


/**
 refreshLoop2 ueberprueft in regelmaessigen Abstaenden, ob ein neuer Aktualisierungs-Request
 gestartet werden kann.
 */
hamster.main.refreshLoop2 = function () {
    hamster.main.sendRefresh();
    window.setTimeout(hamster.main.refreshLoop2, hamster.main.refreshTime);
}

hamster.main.sendRefresh = function () {
    if (hamster.main.websocket) {
        var url = hamster.Config.URL_refresh + "&active=" + hamster.main.active;
        hamster.main.doSilentRequest(url, 'GET', null, false);
    } else if (!hamster.main.isRunningRequest && !hamster.main.makingRefresh) {
//nur wenn nichts mehr in der Warteschleife ist
        if (hamster.main.worker_count >= hamster.main.queue_count) {
            hamster.main.makingRefresh = true;
            hamster.main.doSilentRequest(hamster.Config.URL_refresh, 'GET', null, false);
        }
    }
}

/**
 Auxiliary-Funktion zum Aufruf von hamster.main.doRequest
 */
hamster.main.doRequestAuxiliary = function (url) {
//alert('doRequestAuxiliary'+url);
    hamster.main.doRequest(url, 'GET', null, false);
}

hamster.main.doSilentRequest = function (url, meth, sendData, isFormData) {
    var silent = hamster.silentRequest;
    hamster.silentRequest = true;
    hamster.main.doRequest(url, meth, sendData, isFormData);
    hamster.silentRequest = silent;
}
/**
 Request an den Server
 */
hamster.main.doRequest = function (url, meth, sendData, isFormData) {
    if (typeof(WebSocket) == "function" && !hamster.main.websocketFailed) {
//send over websocket if possible        
        hamster.main.doWebSocketRequest(url, meth, sendData, isFormData);
    } else {

//alert('doRequest url:'+url+" meth: "+meth+" sendData: "+sendData+" isFormData: "+isFormData);
        if (hamster.main.http_request && hamster.main.http_request.abort) {
            hamster.main.http_request.abort();
        }
        hamster.main.queue[hamster.main.queue_count] = new hamster.main.Entry(url, meth, sendData, isFormData);
        hamster.main.queue_count++;
//  hamster.main.doRequestReal(url,meth,sendData,isFormData);  
    }

}

hamster.main.doWebSocketRequest = function (url, meth, sendData, isFormData) {
    var message = url + "&" + sendData;

    try {
        hamster.main.log("doWebSocketRequest: sending message to websocket: " + message);
        if (hamster.main.websocket != null && hamster.main.websocket.readyState == 1) {
            if (url.indexOf("?c?") == -1) {
                if (!hamster.silentRequest) {
                    document.body.style.cursor = "wait";
                    if (hamster.activateLoader) {
                        hamster.main.loaderTimeout = window.setTimeout("hamster.activateLoader()", 1000);
                    }
                }
                hamster.main.websocket.send(message);
            }
        } else {
            hamster.main.log("doWebSocketRequest: websocket is not ready yet, retry in when open again: " + message);
            hamster.main.addToPendingQueue(message); 
//            if (hamster.main.websocket.readyState > 1) {
//                hamster.main.initWebSocket();
//            }
        }
    } catch (e) {      
        hamster.main.log("doWebSocketRequest: rror while trying to send message to websocket,retry in when open again: " + message);
        hamster.main.addToPendingQueue(message); 
    }
}


hamster.main.addToPendingQueue = function (message) {
    for (var i = 0; i < hamster.main.websocket_pending_queue.length; i++) {
        var existing = hamster.main.websocket_pending_queue[i];
        if (existing == message) {
            //do not add duplicates
            return;
        }
    }
    hamster.main.websocket_pending_queue[hamster.main.websocket_pending_queue.length] = message;
}

/**
 
 */
hamster.main.worker = function () {
    try {
        if (hamster.main.worker_count < hamster.main.queue_count) {
            if (hamster.main.wait_count > hamster.main.worker_count - 1) {
//  hamster.main.PAGELOADER_IMG.setAttribute("src", "grafx/pageld.gif");
                document.body.style.cursor = "wait";
                /*hamster.mainAnim.setAJAXLoaderText("lade Daten");
                 hamster.mainAnim.showAJAXLoader();*/

                if (hamster.activateLoader) {
                    hamster.main.loaderTimeout = window.setTimeout("hamster.activateLoader()", 1000);
                }

            }
            if (!hamster.main.isRunningRequest) {
                hamster.main.doRequestReal(hamster.main.queue[hamster.main.worker_count].url, hamster.main.queue[hamster.main.worker_count].meth, hamster.main.queue[hamster.main.worker_count].sendData, hamster.main.queue[hamster.main.worker_count].isFormData, true);
                hamster.main.worker_count++;
            }
        }
    } catch (e) {
        hamster.main.log(e);
    }
    window.setTimeout("hamster.main.worker()", 100);
}

/**
 Request-Object
 */
hamster.main.Entry = function (url, meth, sendData, isFormData) {
    this.url = url;
    this.meth = meth;
    this.sendData = sendData;
    this.isFormData = isFormData;
}

/**
 Request an den Server
 */
hamster.main.doRequestReal = function (url, meth, sendData, isFormData, lock) {
    try {
        hamster.main.http_request = false;
        if (lock) {
            hamster.main.isRunningRequest = true;
        }
        if (hamster.main.websocket) {
//send over websocket if possible
            var message = url + "&" + sendData;
            hamster.main.log("sending message to websocket: " + message);
            hamster.main.doWebSocketRequest(url, meth, sendData, isFormData);
        } else {
//otherwise fallback to AJAX long polling
            if (window.XMLHttpRequest) { // Mozilla, Safari,...
                hamster.main.http_request = new XMLHttpRequest();
                if (hamster.main.http_request.overrideMimeType) {
                    hamster.main.http_request.overrideMimeType('application/xml');
                    // zu dieser Zeile siehe weiter unten
                }
            } else if (window.ActiveXObject) { // IE6
                try {
                    hamster.main.http_request = new ActiveXObject("MSXML2.XMLHTTP.3.0");
                } catch (e) {
                    hamster.main.log(e);
                    if (hamster.main.debug)
                        alert("hamster.main.doRequest(): MSXML Error: Redmond stinkt");
                    try {
                        hamster.main.http_request = new ActiveXObject("Microsoft.XMLHTTP");
                    } catch (e) {
                        hamster.main.log(e);
                    }
                }
            }

            if (!hamster.main.http_request) {
                if (hamster.main.debug)
                    alert('hamster.main.doRequest(): Ende :( Kann keine XMLHTTP-Instanz erzeugen');
                return false;
            }


            hamster.main.http_request.onreadystatechange = hamster.main.syncInhalt;
            //  if(hamster.main.debug)alert("request: meth: "+meth+"  url: "+url+"  "+" sendData: "+sendData);
            hamster.main.http_request.open(meth, url, true);
            if (isFormData) {
                hamster.main.http_request.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
                hamster.main.http_request.setRequestHeader("Content-length", sendData.length);
                hamster.main.http_request.setRequestHeader("Connection", "close");
            }
            hamster.main.http_request.setRequestHeader("Pragma", "no-cache");
            hamster.main.http_request.setRequestHeader("Cache-Control", "must-revalidate");
            hamster.main.http_request.setRequestHeader("If-Modified-Since", document.lastModified);
            hamster.main.http_request.send(sendData);
        }
    } catch (e) {
        hamster.main.log(e);
        if (hamster.main.debug)
            alert("hamster.main.doRequest(): allgemeiner Fehler 1: " + e);
        if (lock) {
            hamster.main.isRunningRequest = false;
        }
    }
}

/**
 hamster.main.syncInhalt():
 behandelt das Eintreffen des XML
 */
hamster.main.syncInhalt = function () {
//alert('snync inhalt');
    try {
        if (hamster.main.http_request.readyState == 4) {
            if (hamster.main.http_request.status == 200) {
                try {
//alert("sync hinhalt status 200");
                    if (hamster.main.http_request.responseText == 'h') {
                        if (hamster.main.debug)
                            alert("d-response");
                    } else if (hamster.main.http_request.responseText.lastIndexOf('failed', 0) == 0) {
                        window.location.reload();
                    } else if (hamster.main.http_request.responseText != 'n') {

//alert(hamster.main.http_request.responseText);

                        hamster.main.applyXML(hamster.main.http_request.responseXML);
                    }
                } catch (e) {
                    hamster.main.log(e);
                    //alert("syncInhalt(): "+hamster.main.http_request.responseXML);
                    //  alert("Fehler bei Anfrage zum Server: "+e);
                    if (!hamster.main.error) {
                        hamster.main.error = true;
                        if (hamster.main.debug)
                            alert("error during request to server " + e);
                    }
                    window.location.reload();
                }
            } else {

                if (hamster.main.http_request.status == 400) {
                    if (hamster.main.debug)
                        alert("Connection lost, page will get reloaded. Http-Message: " + hamster.main.http_request.status);
                    window.location.reload();
                }
//window.setTimeout("window.location.reload()", 1000);
//alert('syncInhalt(): Bei dem Request ist ein Problem aufgetreten.'+hamster.main.http_request.status);
            }
            hamster.main.isRunningRequest = false;
            hamster.main.makingRefresh = false;
        } else {

        }
        if (hamster.main.wait_count < hamster.main.worker_count) {
//hamster.main.PAGELOADER_IMG.setAttribute("src", "grafx/pagenold.gif");
            document.body.style.cursor = "";
            if (hamster.deactivateLoader) {
                if (hamster.main.loaderTimeout) {
                    window.clearTimeout(hamster.main.loaderTimeout);
                }
                hamster.deactivateLoader();
            }
        }
    } catch (e) {
        hamster.main.log(e);
        //hamster.mainAnim.setAJAXLoaderText("Probleme mit der Verbindung zum Server. Starte neue Anfrage..");
        //hamster.mainAnim.showAJAXLoader();
        //	 hamster.mainAnim.setAJAXLoaderError();
        if (typeof (hamster) != "undefined") {
            hamster.main.isRunningRequest = false;
            hamster.main.error = false;
            hamster.main.makingRefresh = false;
        }
//   window.location.reload();
    }
}

/**
 hamster.main.applyXML():
 Leitet das XML an weitere Funktionen weiter, die die Abarbeitung uebernehmen.
 */
hamster.main.applyXML = function (xmlDoc) {
    hamster.main.pageUpdateRunning = true;
    var root_node = null;
    try {
        root_node = xmlDoc.getElementsByTagName('root')[0];
    } catch (e) {
        hamster.main.log(e);
        if (!hamster.main.error) {
            hamster.main.error = true;
            if (hamster.main.debug)
                alert("no root node found :-(((");
        }
//     window.location.reload();
        return;
    }
    var confirm_url = ''
    if (!root_node) {
        return;
    }
    try {
        confirm_url = root_node.getAttribute('cu');
    } catch (e) {
        hamster.main.log(e);
        return;
//	if(hamster.main.retries > 5) {
//	    if(!hamster.main.error) {
//		hamster.main.error = true;
//		// if(hamster.main.debug)alert("Die Verbindung zum Server wurde unterbrochen. Die Seite wird neu geladen.");
//		window.location.reload();
//	    }
//	} else {
//	    hamster.main.retries++;
//	    return;
//	}
    }
    hamster.main.retries = 0;
    try {
        hamster.main.handle_replace(root_node);
    } catch (e) {
        hamster.main.log(e);
        if (hamster.main.debug)
            alert("applyXML: exception in hamster.main.handle_replace");
    }
    try {
        hamster.main.handle_append(root_node);
    } catch (e) {
        hamster.main.log(e);
        if (hamster.main.debug)
            alert("applyXML: exception in hamster.main.handle_append");
    }
    hamster.main.pageUpdateRunning = false;
    try {
        hamster.main.handle_scripts(root_node);
    } catch (e) {
        hamster.main.log(e);
        if (hamster.main.debug)
            alert('applyXML: exception in hamster.main.handle_scripts');
    }
//    try {
//handle_statusbar(root_node);

//Zuruecksetzen der pageloader-grafik
    if (hamster.main.wait_count < hamster.main.worker_count) {
//hamster.main.PAGELOADER_IMG.setAttribute("src", "grafx/pagenold.gif");
        document.body.style.cursor = "";
        if (hamster.deactivateLoader) {
            if (hamster.main.loaderTimeout) {
                window.clearTimeout(hamster.main.loaderTimeout);
            }
            hamster.deactivateLoader();
        }
    }
    if (hamster.main.websocket != null) {
        hamster.main.doRequestAuxiliary(confirm_url);
    }

    hamster.main.initsAfterNewXML(root_node);
//    } catch (e) {
//        alert("applyXML: exception in last part "+e);
//    }

}

/**
 hamster.main.initsAfterNewXML():
 In dieser Funktion soll das stehen, was nach der Abarbeitung eines neuen XMLs initalisiert werden soll
 **/
hamster.main.initsAfterNewXML = function (node) {

}

/**
 hamster.main.handle_append():
 Falls das XML Teile enthaelt, die in den DOM-Baum neu eingehaengt werden sollen
 */
hamster.main.handle_append = function (rootnode) {
//alert("handle_append()");
    var append_nodes = rootnode.getElementsByTagName('append');
    for (var i = 0; i < append_nodes.length; i++) {
        try {
            var append_node = append_nodes.item(i);
            var id = append_node.getAttribute("id");
            var replacement = append_node.getElementsByTagName('r').item(0).firstChild;
            var id_elem = document.getElementById(id);
            //in this case
            if (id_elem != null) {

                var postlistTags = append_node.getElementsByTagName('postlist');
                if (postlistTags != null && postlistTags.length > 0) {

                } else {
                    var newNode = hamster.main.convertToHTMLNode(replacement);
                    id_elem.parentNode.appendChild(newNode);
                }

            } else {
//alert("hamster.main.handle_append() parent id not found");
                hamster.main.log("hamster.main.handle_append() parent id not found");
                hamster.main.convertToHTMLNode(replacement);
            }
        } catch (e) {
            hamster.main.log(e);
            if (hamster.main.debug)
                alert("hamster.main.handle_append(): Fehler: " + e);
        }
    }
}

/**
 hamster.main.handle_replace():
 Falls das XML Teile enthaelt, die bereits bestehende Teile im DOM ersetzen sollen
 */
hamster.main.handle_replace = function (rootnode) {
//alert("hamster.main.handle_replace()");
    var replace_nodes = rootnode.getElementsByTagName('replace');
    for (var x = 0; x < 2; x++) {
        for (var i = 0; i < replace_nodes.length; i++) {

            try {
                var replace_node = replace_nodes.item(i);
                var id = replace_node.getAttribute("id");
                var replacement = replace_node.getElementsByTagName('r').item(0).firstChild;
                var id_elem = document.getElementById(id);
                if (id_elem != null) {

//Check for animations
                    var animationTags = replace_node.getElementsByTagName('animation');
                    if (animationTags != null && animationTags.length > 0) {
//invoke which function?
                        var jsFunction = replace_node.getElementsByTagName('animation').item(0).getAttribute("jsFunction");
                        //collect all params
                        var attribs = replace_node.getElementsByTagName('animation').item(0).attributes;
                        var attributes;
                        attributes = {};
                        for (a = 0; a < attribs.length; a++) {
                            eval("attributes." + attribs[a].nodeName + "=" + attribs[a].nodeValue);
                        }
//invoke function
                        eval(jsFunction + "(replacement, id, attributes)");
                    } else {
//no animation, just replace
                        var newNode = hamster.main.convertToHTMLNode(replacement);
                        id_elem.parentNode.replaceChild(newNode, id_elem);
                        // id_elem.parentNode.replaceChild(replacement, document.getElementById(id));

                    }
                } else {
//only cache it
                    hamster.main.convertToHTMLNode(replacement);
                }
            } catch (e) {
                hamster.main.log(e);
                if (hamster.main.debug)
                    alert("hamster.main.handle_replace(): Fehler: " + e);
            }
        }
    }
}


/**
 hamster.main.handle_scripts():
 Dynamisches Einbinden von Scripts
 */
hamster.main.handle_scripts = function (rootnode) {
    var script_nodes = rootnode.getElementsByTagName('script');
    for (var i = 0; i < script_nodes.length; i++) {
        var script_node = script_nodes.item(i);
        //alert("execute: "+decodeURIComponent(script_node.firstChild.data));
        try {
            var script = decodeURIComponent(script_node.firstChild.data);
            eval(script);
        } catch (e) {
            hamster.main.log(e);
            if (hamster.main.debug)
                alert("hamster.main.handle_scripts(): Error: " + e);
        }
    }
}

/**
 Loeschen von Objecten aus dem DOM
 */
hamster.main.removeObject = function (type, args, obj) {
    obj.parentNode.removeChild(obj);
}

/**
 Hilfsfunktion zum Checken ob Attribut ein Event-Handler ist
 */
hamster.main.checkForEvents = function (attrname) {
    for (var i = 0; i < hamster.main.EVENTS.length; i++) {
        if (hamster.main.EVENTS[i] == attrname)
            return true;
    }
    return false;
}



hamster.main.makeAttribute = function (htmlNode, attrNode) {
    if (attrNode.nodeValue.indexOf("SLOT") != -1 || !hamster.main.checkForEvents(attrNode.nodeName)) {
        try {
            attrName = attrNode.nodeName;
            attrValue = decodeURIComponent(attrNode.nodeValue);
            if (attrName == "style" && attrNode.nodeValue.indexOf("SLOT") == -1) {
                hamster.main.applyStyle(htmlNode, attrValue);
            } else {
                htmlNode.setAttribute(attrName, attrValue + "");
                var attr = htmlNode.getAttributeNode(attrName);
                if (!attr) {
                    attr = document.createAttribute(attrName);
                    htmlNode.setAttributeNode(attr);
                }

                attr.nodeValue = attrValue;
            }

        } catch (e) {
            if (hamster.main.debug)
                alert("createAttribute failed 2: " + htmlNode.getAttribute("class") + " -- " + attrNode.nodeName + " -- " + attrNode.nodeValue + "   " + e);
            var attr = document.createAttribute(attrNode.nodeName);
            if (hamster.main.debug)
                alert("attr created");
            attr.nodeValue = decodeURIComponent(attrNode.nodeValue);
            if (hamster.main.debug)
                alert("value set");
            htmlNode.setAttributeNode(attr);
        }
    } else {
        try {
            htmlNode[attrNode.nodeName] = new Function("", decodeURIComponent(attrNode.nodeValue));
        } catch (e) {
            if (hamster.main.debug)
                alert("Function creation failed: " + attrNode.nodeName);
            hamster.main.log("Function creation failed: " + attrNode.nodeName);
            return;
        }
    }
}
/**
 hamster.main.convertToHTMLNode():
 Uebersetzen des XML in den DOM-Baum
 */
hamster.main.convertToHTMLNode = function (xmlNode) {
    try {
        if (xmlNode.nodeName.indexOf("#") != -1) {
            try {
                return document.createTextNode(decodeURIComponent(xmlNode.data));
            } catch (e) {
                if (hamster.main.debug)
                    alert("Text could not be converted " + xmlNode.data + "  in+" + xmlNode.parentNode.nodeName + " " + xmlNode.parentNode.getAttribute("id") + " " + xmlNode.parentNode.getAttribute("class"));
            }

        } else if (xmlNode.nodeName == "cache") {
//Knoten aus dem Cache holen:
            var newHTMLNode = hamster.main.Cache[xmlNode.getAttribute("id")];
            if (newHTMLNode == null) {
//element anfordern
                hamster.main.doRequestAuxiliary(hamster.Config.URL_refresh + "?" + xmlNode.getAttribute("id"));
                window.location.reload();
                // return document.createTextNode("Object is missing in cache" + xmlNode.getAttribute("id") + " loading now..");
            }
            return newHTMLNode;
        } else if (xmlNode.nodeName == "content") {
            var newHTMLNode = document.createElement("span");
            var encoded = "";
            for (var i = 0; i < xmlNode.childNodes.length; i++) {
                encoded += xmlNode.childNodes[i].data;
            }
            var decodedHTML = decodeURIComponent(encoded);
            newHTMLNode.innerHTML = decodedHTML;
            return newHTMLNode;
        } else {

            try {
                var newHTMLNode = document.createElement(xmlNode.nodeName);
            } catch (e) {
                if (hamster.main.debug)
                    alert("document.createElement failed");

                hamster.main.log("hamster.main.convertToHTMLNode() document.createElement failed " + xmlNode.nodeName + " \n " + e);
                return;
            }

            for (var i = 0; i < xmlNode.childNodes.length; i++) {
                try {
                    newHTMLNode.appendChild(hamster.main.convertToHTMLNode(xmlNode.childNodes[i]));
                } catch (e) {
                    if (hamster.main.debug)
                        alert("appendChild failed: " + xmlNode.childNodes[i].nodeName + ": " + xmlNode.childNodes[i].nodeValue);
                    return;
                }
            }
            for (var i2 = 0; i2 < xmlNode.attributes.length; i2++) {
                hamster.main.makeAttribute(newHTMLNode, xmlNode.attributes[i2]);
            }

            /*if (navigator.appName == "Microsoft Internet Explorer" && newHTMLNode.nodeName == 'A') {
             newHTMLNode.attachEvent('onclick',hamster.main.changeToXLinkIE);                            
             }*/
            try {
//add HTML Node to Cache:
                if (xmlNode.getAttribute("id") && xmlNode.nodeName != "template") {
                    if (xmlNode.getAttribute("id").charAt(0) != "L") {
                        hamster.main.Cache[xmlNode.getAttribute("id")] = newHTMLNode;
                    }
                }
            } catch (e) {
                if (hamster.main.debug)
                    alert("adding to cache failed. id: " + xmlNode.getAttribute("id"));
                hamster.main.log("adding to cache failed. id: " + xmlNode.getAttribute("id"));
                return;
            }

            return newHTMLNode;
        }
    } catch (e) {
        if (hamster.main.debug)
            alert("hamster.main.convertToHTMLNode(): " + xmlNode.nodeName + " \n " + e);
        hamster.main.log("hamster.main.convertToHTMLNode(): " + xmlNode.nodeName + " \n " + e);
    }
    return document.createTextNode("Invalid XML for this Element");
}


/**
 
 */
hamster.main.changeToXLink = function (id, request, static_url) {
    document.getElementById(id).href = "javascript:hamster.main.changeToLink('" + id + "','" + request + "','" + static_url + "')";
}

hamster.main.changeToLink = function (id, request, static_url) {
    orequest = request;
//    request = request.replace("?x","?k");
//    request = request.replace("?a","?k");
    document.getElementById(id).href = request;
    hamster.main.wait_count = hamster.main.queue_count;
    if (hamster.activateAlwaysLoader) {
        hamster.activateAlwaysLoader();
    }
    try {
        var animation=document.getElementById(id).dataset.animation;
        eval(animation);
    } catch (e) {
          hamster.main.log("changeToLink animiation error: "+e);   
    }
//normaler Komponenten-XML-Aufruf
    hamster.main.doRequestAuxiliary(request);
    if (static_url != "") {
        window.location.hash = static_url;
        hamster.main.currentPath = static_url;
    }

}

hamster.main.changeToZLink = function (id, static_path) {
    document.getElementById(id).href = "javascript:hamster.main.changeToLink('" + id + "','" + hamster.Config.URL_static + "?" + static_path + "')";
}

/**
 hamster.main.postForm();
 */
hamster.main.postForm = function (url, formid, reset) {
    hamster.main.postColorsAndForm(url, formid, "", reset);
}

hamster.main.postFormWithRTE = function (url, formid, editorId, hiddenInputId, reset) {
    rte.submitWysiwyg(formid, editorId, hiddenInputId);
    hamster.main.postColorsAndForm(url, formid, "", reset);
}



/**
 hamster.main.postColorsAndForm();
 
 uebermittelt zusaetzlich zu einem Formular noch Farbwerte, die mit dem Colorpicker bearbeitet wurden
 */
hamster.main.postColorsAndForm = function (url, formid, post_str, reset) {
    if (hamster.main.onkeyTimeout) {
        window.clearTimeout(hamster.main.onkeyTimeout);
    }
    var poststr = post_str;
    var formular = document.getElementById(formid);
    for (var i = 0; i < formular.elements.length; i++) {
        if (formular.elements[i].getAttribute("type") == "password") {
            poststr += formular.elements[i].name + "=" + MD5(formular.elements[i].value);
            if (i < formular.elements.length)
                poststr += "&";
        } else if (formular.elements[i].getAttribute("type") == "checkbox") {
//   alert('checkbox');
            poststr += formular.elements[i].name + "=" + encodeURIComponent(formular.elements[i].checked);
            if (i < formular.elements.length)
                poststr += "&";
        } else if (formular.elements[i].getAttribute("type") == "radio") {
            var radio = formular.elements[i];
            if (radio.checked == true) {
                poststr += radio.name + "=" + encodeURIComponent(radio.value);
                if (i < formular.elements.length)
                    poststr += "&";
            }

        } else if (formular.elements[i].getAttribute("type") == "select-one") {
            var elm = formular.elements[i];
            poststr += formular.elements[i].name + "=" + encodeURIComponent(formular.elements[i].options[formular.elements[i].selectedIndex].innerText);
            if (i < formular.elements.length)
                poststr += "&";
        } else if (formular.elements[i].getAttribute("type") != "image") {
            var elm = formular.elements[i];
            poststr += formular.elements[i].name + "=" + encodeURIComponent(formular.elements[i].value);
            if (i < formular.elements.length)
                poststr += "&";
        }
//alert('encoded: '+formular.elements[i].name+"="+encodeURIComponent(formular.elements[i].value));
    }
    var textareas = formular.getElementsByClassName("textarea");
    for (var i = 0; i < textareas.length; i++) {
        var elm = textareas[i];
        poststr += textareas[i].getAttribute("name") + "=" + encodeURIComponent(textareas[i].innerHTML);
        if (reset) {
            textareas[i].innerHTML = "<br/>";
        }
        if (i < formular.elements.length)
            poststr += "&";
    }
// hamster.main.wait_count = hamster.main.queue_count;
    if (reset) {
        formular.reset();
    }
    hamster.main.doRequest(url, 'POST', poststr, true);
// hamster.main.doRequest(url,'GET',null,false);
}


/**
 hamster.main.postJSObject();
 
 Die Methode zerlegt das JS-Object obj und ueberfuehrt es in Parameter, welche dem Framwork uebergeben werden.
 */
hamster.main.postJSObject = function (url, obj) {
    var poststr = "";
    for (var name in obj) {
        if (poststr == "") {
            poststr += name + "=" + obj[name];
        } else {
            poststr += "&" + name + "=" + obj[name];
        }
    }
//alert(poststr);
// hamster.main.wait_count = hamster.main.queue_count;
//hamster.mainAnim.setAJAXLoaderAction();
    hamster.main.doRequest(url, 'POST', poststr, true);
}



hamster.main.selectRadio = function (formid, radio, value) {
    var formular = document.getElementById(formid);
    for (var i = 0; i < formular.elements.length; i++) {
        if (formular.elements[i].getAttribute("type") == "radio" && formular.elements[i].getAttribute("name") == radio) {
            var r = formular.elements[i];
            if (r.value == value) {
                r.checked = true;
            }
        }
    }
}


hamster.main.checkAllBoxes = function (formid) {
    var formular = document.getElementById(formid);
    for (var i = 0; i < formular.elements.length; i++) {
        if (formular.elements[i].getAttribute("type") == "checkbox") {
            formular.elements[i].checked = true;
        }
    }
}

hamster.main.loadImage = function (src) {
    var image = new Image();
    image.src = src;
}

hamster.main.submitWithTimeout = function (formId, posturl) {
    if (hamster.main.onkeyTimeout) {
        window.clearTimeout(hamster.main.onkeyTimeout);
    }
    hamster.main.onkeyTimeout = window.setTimeout(hamster.main.autoSubmitForm, 300, formId, posturl);
}

hamster.main.autoSubmitForm = function (formId, posturl) {
    var form = document.getElementById(formId);
    hamster.main.postColorsAndForm(posturl, formId, posturl + "&autosubmit=yes&", false);
}


hamster.main.submitForm = function (formId) {
    var form = document.getElementById(formId);
    form.submit();
}

hamster.main.log = function (logMessage) {
    if (window.console && window.console.log) {
// console is available
        window.console.log(logMessage);
    }
}

hamster.main.activeTimeout = null;
hamster.main.setActive = function () {
    var old = hamster.main.active;
    hamster.main.active = true;
    if (hamster.main.activeTimeout != null) {
        clearTimeout(hamster.main.activeTimeout);
        hamster.main.activeTimeout = null;
    }
    hamster.main.activeTimeout = setTimeout(function () {
        hamster.main.setInactive()
    }, 30000);
    //send the server, that we are alive again
    if (!old) {
        hamster.main.log("user is marked as active now");
        hamster.main.sendRefresh();
    }
}

hamster.main.setInactive = function () {
    var old = hamster.main.active;
    if (hamster.main.activeTimeout != null) {
        clearTimeout(hamster.main.activeTimeout);
        hamster.main.activeTimeout = null;
    }
    hamster.main.active = false;
    if (old) {
        hamster.main.log("user is marked as inactive now");
        hamster.main.sendRefresh();
    }
}

hamster.main.setFocus = function (formId) {
    if (!hamster.main.focusStealingBlocked) {
        document.getElementById(formId).focus();
    }
}

hamster.main.blockFocus = function () {
    hamster.main.focusStealingBlocked = true;
    hamster.main.log("blocking focus");
}

hamster.main.unblockFocus = function () {
    hamster.main.focusStealingBlocked = false;
    hamster.main.log("unblocking focus");
}



