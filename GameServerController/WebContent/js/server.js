function runCommand(command, serverID) {
	if(!isActionProcessing(serverID)) {
		if(command == "start" && isRunning(serverID)) {
			return;
		}
		if(command == "stop" && !isRunning(serverID)) {
			return;
		}
		
		if(command == "start" || command == "stop" || command == "restart") {
			serverStatus[serverID].action = command;
		}
		
		var url = encodeURI(serverCommandEndpoint + "?id=" + serverID + "&command=" + command);
		var spinner = document.getElementById("spinner-" + command + "-" + serverID);
		if(spinner != null) {
			spinner.hidden = false;
		}
		return fetch(url);
	}
	
	return null;
}

function isActionProcessing(serverID) {
	if(serverID in serverStatus) {
		return serverStatus[serverID].action != '';
	}

	return false;
}

function isRunning(serverID) {
	if(serverID in serverStatus) {
		return serverStatus[serverID].running;
	}
	
	return false;
}

function clearAction(serverID) {
	if(serverID in serverStatus) {
		serverStatus[serverID].action = '';
	}
}

function intializeSockets(idToAddresses, callback) {
	Object.entries(idToAddresses).forEach(([serverID, address]) => {
		let newSocket = new WebSocket(address);
		serverStatus[serverID] = {running: false, action: '', socket: newSocket};
		newSocket.onmessage = function(event) {
			callback(event, serverID);
		};
	});
}

var serverStatus = {};