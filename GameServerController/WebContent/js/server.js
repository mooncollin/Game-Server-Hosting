function runCommand(serverID, values, callback) {
	if(!isActionProcessing(serverID)) {
		if(values.command == "start" && isRunning(serverID)) {
			return;
		}
		if(values.command == "stop" && !isRunning(serverID)) {
			return;
		}
		
		if(values.type === 'action') {
			serverStatus[serverID].action = values.command;
		}
		
		var url = encodeURI(serverCommandEndpoint + "?id=" + serverID);
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = callback;
		xhr.open("POST", url, true);
		xhr.setRequestHeader('Content-Type', 'application/json');
		xhr.send(JSON.stringify(values));
		return xhr;
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