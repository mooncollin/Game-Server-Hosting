function startServer() {
	if(startSpinner.hidden) {
		startSpinner.hidden = false;
		initializeSocket();
		logBox.value = "";
		fetch(serverStartRequest);
	}
}

function stopServer() {
	if(stopSpinner.hidden) {
		stopSpinner.hidden = false;
		initializeSocket();
		fetch(serverStopRequest);
	}
}

function command(event) {
	if(event.key === "Enter" && commandBox.value != "" && !stopButton.disabled) {
		fetch(serverCommandRequest + commandBox.value);
		previousCommands.splice(previousCommands.length - 1, 0, commandBox.value);
		if(previousCommands.length >= 50)
		{
			previousCommands.splice(previousCommands.length - 2, 1);
		}
		previousCommandsIndex = previousCommands.length - 1;
		commandBox.value = "";
	}
}

function history(event) {
	if(event.key === "ArrowUp") {
		previousCommandsIndex--;
		if(previousCommandsIndex < 0) {
			previousCommandsIndex = 0;
		}
		commandBox.value = previousCommands[previousCommandsIndex];
	}
	else if(event.key === "ArrowDown") {
		previousCommandsIndex++;
		if(previousCommandsIndex >= previousCommands.length) {
			previousCommandsIndex = previousCommands.length - 1;
		}
		commandBox.value = previousCommands[previousCommandsIndex];
	}
}

function updateStatus() {
	if(isRunning) {
		startButton.disabled = true;
		stopButton.disabled = false;
		startSpinner.hidden = true;
	}
	else {
		stopButton.disabled = true;
		startButton.disabled = false;
		stopSpinner.hidden = true;
	}
}

function initializeSocket() {
	if(socket === undefined || socket.readyState === socket.CLOSED) {
		socket = new WebSocket(socketAddress);
		socket.onmessage = function(event) {
			serverData = event.data;
			if(serverData instanceof Blob) {
				serverData.text().then(function(text) {
					updateScroll();
					let currentScrolledUp = scrolledUp;
					if(currentLineAmount >= MAX_LINE_AMOUNT) {
						logBox.value = logBox.value.substring(nthIndex(logBox.value, "\n", 1));
						currentLineAmount -= 1;
						if(!currentScrolledUp) {
							logBox.scrollTop = logBox.scrollHeight;
						}
					}
					
					logBox.value += text;
					currentLineAmount++;
					updateScroll();
					if(!currentScrolledUp) {
						logBox.scrollTop = logBox.scrollHeight;
					}
				})
			}
			else {
				if(serverData === "<on>") {
					isRunning = true;
					updateStatus();
				}
				else if(serverData === "<off>") {
					isRunning = false;
					updateStatus();
				}
			}
		};
	}
}

function nthIndex(str, pat, n) {
	let i = -1;
	while(n-- && i++ < str.length) {
		i = str.indexOf(pat, i);
		if(i < 0) break;
	}
	return i;
}

function updateScroll() {
	if(logBox.scrollTop < lastScrollUp) {
		scrolledUp = true;
	}
	else if(logBox.scrollHeight <= logBox.scrollTop + logBox.offsetHeight) {
		scrolledUp = false;
	}
	if(!scrolledUp) {
		logBox.scrollTop = logBox.scrollHeight;
	}
	lastScrollUp = logBox.scrollTop;
}

function editRow(button, triggerID, id) {
	let table = tables[id];
	button.innerHTML = '';
	
	let row = findRowById(table, triggerID)

	let rowType = row.cells[0].textContent;
	
	for(let colIndex = 1; colIndex < row.cells.length - 1; colIndex++) {
		let col = row.cells[colIndex];
		col.innerHTML = '';
	}
	
	if(button.classList.contains("btn-warning")) {
		button.classList.remove("btn-warning");
		button.classList.add("btn-primary");
		button.appendChild(editIconClose.cloneNode(true));
		
		let input;
		if(rowType === "Time") {
			input = valueTime.cloneNode(true);
		}
		else if(rowType === "Output") {
			input = valueText.cloneNode(true)
		}
		else if(rowType === "Recurring") {
			input = recurringText.cloneNode(true);
		}
		
		input.value = triggerOldValues[triggerID]["value"];
		input.setAttribute('form', id + triggerID);
		input.onkeydown = function(event) {
			if(event.key === 'Enter') {
				document.getElementById(input.getAttribute('form')).submit();
			}
		}
		
		row.cells[1].appendChild(input);
		let commandTextClone = commandText.cloneNode(true);
		commandTextClone.value = triggerOldValues[triggerID]["command"];
		commandTextClone.setAttribute('form', id + triggerID);
		commandTextClone.onkeydown = function(event) {
			if(event.key === 'Enter') {
				document.getElementById(commandTextClone.getAttribute('form')).submit();
			}
		}
		
		row.cells[2].appendChild(commandTextClone);
		let actionSelectClone = actionSelect.cloneNode(true);
		actionSelectClone.setAttribute('form', id + triggerID);
		for(let i = 0; i < actionSelectClone.options.length; i++) {
			if(actionSelectClone.options[i].value === triggerOldValues[triggerID]["action"]) {
				actionSelectClone.selectedIndex = i;
				break;
			}
		}
		
		actionSelectClone.onkeydown = function(event) {
			if(event.key === 'Enter') {
				document.getElementById(actionSelectClone.getAttribute('form')).submit();
			}
		}
		row.cells[3].appendChild(actionSelectClone);
	}
	else {
		button.classList.remove("btn-primary");
		button.classList.add("btn-warning");
		button.appendChild(editIcon.cloneNode(true));
		row.cells[1].innerHTML = triggerOldValues[triggerID]["value"];
		row.cells[2].innerHTML = triggerOldValues[triggerID]["command"];
		row.cells[3].innerHTML = triggerOldValues[triggerID]["action"];
	}
}

function deleteRow(button) {
	if(confirm("Are you sure you want to delete this trigger?")) {
		window.location.href = button.getAttribute('link');
	}
}

function addTrigger(button) {
	let table = tables[button.getAttribute("trigger")];
	if(table.rows[table.rows.length-1].cells[0].children.length > 0)
	{
		table.deleteRow(table.rows.length-1);
		return;
	}
	let row = table.insertRow();
	
	let typeCell = row.insertCell();
	let valueCell = row.insertCell();
	let commandCell = row.insertCell();
	let actionCell = row.insertCell();
	
	let typeInput = typeSelect.cloneNode(true);
	typeInput.setAttribute('form', newTriggerFormID);
	let valueInput = recurringText.cloneNode(true);
	valueInput.setAttribute('form', newTriggerFormID);
	let commandInput = commandText.cloneNode(true);
	commandInput.setAttribute('form', newTriggerFormID);
	let actionInput = actionSelect.cloneNode(true);
	actionInput.setAttribute('form', newTriggerFormID);
	
	typeInput.onkeydown = function(event) {
		if(event.key === 'Enter') {
			document.getElementById(typeInput.getAttribute('form')).submit();
		}
	}
	
	typeInput.onchange = function(event) {
		let type = typeInput.options[typeInput.selectedIndex].text;
		let input;
		if(type === "Time") {
			input = valueTime.cloneNode(true);
		}
		else if(type === "Output") {
			input = valueText.cloneNode(true);
		}
		else if(type === "Recurring") {
			input = recurringText.cloneNode(true);
		}
		input.setAttribute('form', newTriggerFormID);
		input.onkeydown = function(event) {
			if(event.key === 'Enter') {
				document.getElementById(valueInput.getAttribute('form')).submit();
			}
		}
		valueCell.innerHTML = '';
		valueCell.appendChild(input);
	}
	
	valueInput.onkeydown = function(event) {
		if(event.key === 'Enter') {
			document.getElementById(valueInput.getAttribute('form')).submit();
		}
	}
	
	commandInput.onkeydown = function(event) {
		if(event.key === 'Enter') {
			document.getElementById(commandInput.getAttribute('form')).submit();
		}
	}
	
	actionInput.onkeydown = function(event) {
		if(event.key === 'Enter') {
			document.getElementById(actionInput.getAttribute('form')).submit();
		}
	}
	
	typeCell.appendChild(typeInput);
	valueCell.appendChild(valueInput);
	commandCell.appendChild(commandInput);
	actionCell.appendChild(actionInput);
}

function findRowById(table, id) {
	for(let i = 0; i < table.rows.length; i++) {
		if(table.rows[i].id == id) {
			return table.rows[i];
		}
	}
	
	return null;
}

function getTableDataText(element) {
	let elementFirst = element.firstChild;
	
	if(elementFirst == null) {
		return "";
	}
	else if(elementFirst instanceof HTMLInputElement) {
		return elementFirst.value;
	}
	else if(elementFirst instanceof HTMLSelectElement) {
		return elementFirst.options[elementFirst.selectedIndex].text;
	}
	
	return elementFirst.textContent;
}

var isRunning = false;
var scrolledUp = false;
var previousCommands = [""];
var previousCommandsIndex = 0;
var logBox = document.getElementById("log");
logBox.onscroll = updateScroll;
var commandBox = document.getElementById("command");
var startButton = document.getElementById("start");
var startSpinner = startButton.children[0];
var stopButton = document.getElementById("stop");
var stopSpinner = stopButton.children[0];
startButton.disabled = true;
stopButton.disabled = true;
var lastScrollUp = logBox.scrollTop;
var currentLineAmount = 0;

const MAX_LINE_AMOUNT = 700;

var socket;

var tabs = document.getElementById("tab-contents");
var tables = [];
for(let i = 0; i < tabs.children.length; i++) {
	tables[tabs.children[i].id] = tabs.children[i].getElementsByTagName("table")[0];
}

const editIcon = document.createElement("i");
editIcon.classList.add("fas", "fa-edit");

const editIconClose = document.createElement("i");
editIconClose.classList.add("fas", "fa-times");

const typeSelect = document.createElement("select");
typeSelect.required = true;
typeSelect.classList.add("form-control");
typeSelect.name = "type";
for(let i = 0; i < triggerTypes.length; i++) {
	let option = document.createElement("option");
	option.text = triggerTypes[i];
	typeSelect.add(option.cloneNode(true));
}

const newTriggerFormID = "newTrigger";

const valueTime = document.createElement("input");
valueTime.type = "time";
valueTime.classList.add("form-control");
valueTime.required = true;
valueTime.name = "value";
valueTime.step = 1;

const valueText = document.createElement("input");
valueText.type = "text";
valueText.classList.add("form-control");
valueText.required = true;
valueText.name = "value";

const recurringText = valueText.cloneNode(true);
recurringText.placeholder = "00:00:00";

const commandText = document.createElement("input");
commandText.type = "text";
commandText.classList.add("form-control");
commandText.name = "command";

const actionSelect = document.createElement("select");
actionSelect.name = "action";
actionSelect.classList.add("form-control");
actionSelect.add(document.createElement("option").cloneNode(true));
for(let i = 0; i < actionTypes.length; i++) {
	let option = document.createElement("option");
	option.text = actionTypes[i];
	actionSelect.add(option.cloneNode(true));
}

fetch(serverLastRequest).then(response => {
	if(response.status === 200) {
		response.text().then(text => {
			logBox.value += text;
			currentLineAmount += logBox.value.split("\n").length;
			if(currentLineAmount >= MAX_LINE_AMOUNT) {
				updateScroll();
				logBox.value = logBox.value.substring(nthIndex(logBox.value, "\n", 1));
				currentLineAmount -= 1;
			}
			updateScroll();
			initializeSocket();
		});
	}
});