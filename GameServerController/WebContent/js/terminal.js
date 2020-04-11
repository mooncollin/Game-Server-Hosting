class Terminal {
	constructor(id, commandCallback, terminalOptions, inputOptions) {
		this.terminalOptions = {'attributes' : {'class': 'rounded-lg'}};
		this.inputOptions = {'attributes': {'class': 'form-control'}};
		this.commandCallback = commandCallback;
		this.commandHistoryMax = 50;
		this.lineAmountMax = 700;
		this.currentLineAmount = 0;
		this.scrollMargin = 5;
		
		if(inputOptions.hasOwnProperty('commandHistoryMax')) {
			this.commandHistoryMax = inputOptions.commandHistoryMax;
		}
		if(terminalOptions.hasOwnProperty('lineAmountMax')) {
			this.lineAmountMax = terminalOptions.lineAmountMax;
		}
		if(terminalOptions.hasOwnProperty('scrollMargin')) {
			this.scrollMargin = terminalOptions.scrollMargin;
		}
		
		this.parentElement = document.getElementById(id);
		this.terminal = this.createTerminal();
		this.input = this.createInput(this.commandCallback);
		
		this.parentElement.appendChild(this.terminal);
		this.parentElement.appendChild(this.input);
		
		if(terminalOptions.hasOwnProperty('attributes')) {
			this.updateTerminalAttributes(terminalOptions.attributes);
		}
		else {
			this.updateTerminalAttributes({});
		}
		
		if(inputOptions.hasOwnProperty('attributes')) {
			this.updateInputAttributes(inputOptions.attributes);
		}
		else {
			this.updateInputAttributes({});
		}
	}
	
	updateTerminalAttributes(attributes) {
		Object.keys(this.terminalOptions.attributes).forEach(property => {
			if(attributes.hasOwnProperty(property)) {
				this.terminalOptions.attributes[property] = attributes[property];
			}
		}, this);
		
		let terminalAttributes = Object.keys(this.terminal.attributes);
		
		terminalAttributes.forEach(attribute => {
			this.terminal.removeAttribute(attribute);
		}, this);
		
		Object.keys(this.terminalOptions.attributes).forEach(attribute => {
			this.terminal.setAttribute(attribute, this.terminalOptions.attributes[attribute]);
		});
	}
	
	updateInputAttributes(attributes) {
		Object.keys(this.inputOptions.attributes).forEach(property => {
			if(attributes.hasOwnProperty(property)) {
				this.inputOptions.attributes[property] = attributes[property];
			}
		}, this);
		
		let inputOptions = Object.keys(this.input.attributes);
		
		inputOptions.forEach(attribute => {
			this.input.removeAttribute(attribute);
		}, this);
		
		Object.keys(this.inputOptions.attributes).forEach(attribute => {
			this.input.setAttribute(attribute, this.inputOptions.attributes[attribute]);
		});
	}
	
	print(str, callback) {
		let textDiv = document.createElement('div');
		textDiv.classList.add("terminal-element");
		textDiv.textContent = str;
		let wasAtBottom = this.isTerminalScrolledToBottom();
		this.terminal.appendChild(textDiv);
		if(wasAtBottom) {
			this.scrollToBottom();
		}
		this.currentLineAmount++;
		if(this.currentLineAmount >= this.lineAmountMax) {
			this.terminal.removeChild(this.terminal.firstChild);
		}
		if(callback) {
			callback(textDiv, str);
		}
	}
	
	clear() {
		this.terminal.innerHTML = '';
		this.currentLineAmount = 0;
	}
	
	createTerminal() {
		let terminal = document.createElement('div');
		terminal.id = 'terminal-output';
		terminal.contenteditable = "true";
		terminal.setAttribute('readonly', '');
		return terminal;
	}
	
	createInput(commandCallback) {
		this.previousCommands = [""];
		this.previousCommandsIndex = 0;
		let input = document.createElement('input');
		input.id = 'terminal-input';
		input.setAttribute('type', 'text');
		input.setAttribute('autofocus', 'true');
		
		input.onkeydown = function(event) {
			if(event.key === 'ArrowUp') {
				this.previousCommandsIndex--;
				if(this.previousCommandsIndex < 0) {
					this.previousCommandsIndex = 0;
				}
				
				input.value = this.previousCommands[this.previousCommandsIndex];
			}
			else if(event.key == 'ArrowDown') {
				this.previousCommandsIndex++;
				if(this.previousCommandsIndex >= this.previousCommands.length) {
					this.previousCommandsIndex = this.previousCommands.length - 1;
				}
				
				input.value = this.previousCommands[this.previousCommandsIndex];
			}
		}.bind(this);
		
		input.onkeypress = function(event) {
			if(event.key === 'Enter' && input.value != '') {
				this.previousCommands.splice(this.previousCommands.length - 1, 0, input.value);
				if(this.previousCommands.length >= this.commandHistoryMax) {
					this.previousCommands.splice(this.previousCommands.length - 2, 1);
				}
				this.previousCommandsIndex = this.previousCommands.length - 1;
				this.commandCallback(input.value);
				input.value = '';
				this.scrollToBottom();
			}
		}.bind(this);
		
		return input;
	}
	
	scrollToBottom() {
		this.terminal.scrollTop = this.terminal.scrollHeight - this.terminal.offsetHeight;
	}
	
	isTerminalScrolledToBottom() {
		return this.terminal.scrollTop - (this.terminal.scrollHeight - this.terminal.offsetHeight) >= -this.scrollMargin;
	}
}