var sockets = [];
var serverLocations = [];

for(let i = 0; i < nodeAddresses.length; i++) {
	serverLocations[serverNames[i]] = "/GameServerController/ServerInteract?name=" + serverNames[i]
	let newSocket = new WebSocket(nodeAddresses[i]);
	newSocket.onmessage = function(event) {
		let statusRow = document.getElementById('status-' + serverNames[i]);
		let circle = statusRow.children[0];
		let text = statusRow.children[1];
		if(event.data === "<on>") {
			circle.classList.remove('text-danger');
			circle.classList.add('text-success');
			text.innerHTML = 'Running';
			document.getElementById('start-' + serverNames[i]).disabled = true;
			document.getElementById('stop-' + serverNames[i]).disabled = false;
		}
		else if(event.data === "<off>") {
			circle.classList.remove('text-success');
			circle.classList.add('text-danger');
			text.innerHTML = 'Stopped';
			document.getElementById('start-' + serverNames[i]).disabled = false;
			document.getElementById('stop-' + serverNames[i]).disabled = true;
		}
	}
}

function startServer(serverName) {
	let location = serverLocations[serverName];
	if(location != null && location != undefined) {
		fetch(location + "&command=start");
	}
}

function stopServer(serverName) {
	let location = serverLocations[serverName];
	if(location != null && location != undefined) {
		fetch(location + "&command=stop");
	}
}

function deleteServer(button, serverName) {
	if(confirm("Are you sure you want to delete this server?")) {
		let input = prompt("Please type the name of the server correctly to delete");
		while(input != null && input != serverName)
		{
			input = prompt("Please type the name of the server correctly to delete");
		}
		if(input != null) {
			window.location.href = button.getAttribute('link');
		}
	}
}