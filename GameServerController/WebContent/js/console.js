function getIPAddress(serverID) {
	
	runCommand(serverID, {"command": "ipaddress"}, function() {
		if(this.readyState == 4 && this.status == 200) {
			jsonPayload = JSON.parse(this.responseText);
			var tempInput = document.createElement("input");
			tempInput.value = jsonPayload.result;
			document.body.appendChild(tempInput);
			tempInput.select();
			document.execCommand("copy");
			document.body.removeChild(tempInput);
		}
	});
}

function consoleTextFinalize(div, text) {
	var typeReg = /^\[\d+:\d+:\d+\] \[[\w\s]+\/(\w+)?\]/g;
	var match = typeReg.exec(text);
	if(match != null) {
		var type = match[1];
		if(type == "WARN") {
			div.style.color = "yellow";
		}
		else if(type == "ERROR") {
			div.style.color = "red";
		}
	}
}

var term = new Terminal('terminal', function(command) {
	if(!serverStatus[serverID].running) {
		term.print("Please start server to enter commands");
	}
	else {
		runCommand(serverID, {"command": "input", "value": command});
	}
}, {}, {}); 


var originalStartCommand =  $('#start-' + serverID)[0].onclick;
$('#start-' + serverID)[0].onclick = null;
$('#start-' + serverID).click(function() {
	term.clear();
	originalStartCommand();
});

runCommand(serverID, {"command": "log", "type": "command"}, function() {
	if(this.readyState == 4 && this.status == 200) {
		jsonPayload = JSON.parse(this.responseText);
		if(jsonPayload.result != '') {
			var lines = jsonPayload.result.split("\r\n");
			lines.forEach(async function(line) {
				await term.print($.trim(line), consoleTextFinalize);
			});
		}
			
		term.scrollToBottom();
		
		intializeSockets(nodeOutputAddresses, function(event, serverID) {
			var serverData = event.data;
			if(serverData instanceof Blob) {
				serverData.text().then(async function(text) {
					await term.print($.trim(text), consoleTextFinalize);
				});
			}
			else {
				if(serverData === "<on>") {
					serverStatus[serverID].running = true;
					clearAction(serverID);
					document.getElementById('start-' + serverID).disabled = true;
					document.getElementById('stop-' + serverID).disabled = false;
					document.getElementById('spinner-start-' + serverID).hidden = true;
					document.getElementById('spinner-restart-' + serverID).hidden = true;
				}
				else if(serverData === "<off>") {
					serverStatus[serverID].running = false;
					clearAction(serverID);
					document.getElementById('start-' + serverID).disabled = false;
					document.getElementById('stop-' + serverID).disabled = true;
					document.getElementById('spinner-stop-' + serverID).hidden = true;
				}
			}
		});
	}
});