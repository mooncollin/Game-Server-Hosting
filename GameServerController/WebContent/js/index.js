var sockets = [];

Object.entries(nodeOutputAddresses).forEach(([serverID, address]) => {
	let newSocket = new WebSocket(address);
	newSocket.onmessage = function(event) {
		let statusRow = document.getElementById('status-' + serverID);
		let circle = statusRow.children[0];
		let text = statusRow.children[1];
		if(event.data === "<on>") {
			circle.classList.remove('text-danger');
			circle.classList.add('text-success');
			text.innerHTML = 'Running';
			document.getElementById('start-' + serverID).disabled = true;
			document.getElementById('stop-' + serverID).disabled = false;
		}
		else if(event.data === "<off>") {
			circle.classList.remove('text-success');
			circle.classList.add('text-danger');
			text.innerHTML = 'Stopped';
			document.getElementById('start-' + serverID).disabled = false;
			document.getElementById('stop-' + serverID).disabled = true;
		}
	}
});

function startServer(serverID) {
	let location = startServerAddresses[serverID];
	if(location != null && location != undefined) {
		fetch(location);
	}
}

function stopServer(serverID) {
	let location = stopServerAddresses[serverID];
	if(location != null && location != undefined) {
		fetch(location);
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