var typeSelect = document.getElementById("type");
var nodeSelect = document.getElementById("node");
var nodeSmall = nodeSelect.parentElement.getElementsByTagName("small")[0];
typeSelect.selectedIndex = 0;
var allFields = [["minecraft", document.getElementsByClassName("minecraft-type")]];
changeNode();

function changeType() {
	let typeValue = typeSelect.options[typeSelect.selectedIndex].value;
	console.log(typeValue);
	for(let i = 0; i < allFields.length; i++) {
		if(allFields[i][0] === typeValue) {
			for(let j = 0; j < allFields[i][1].length; j++) {
				allFields[i][1][j].hidden = false;
			}
		}
		else {
			for(let j = 0; j < allFields[i][1].length; j++) {
				allFields[i][1][j].hidden = true;
			}
		}
	}
}

function changeNode() {
	let selectedOption = nodeSelect.options[nodeSelect.selectedIndex];
	nodeSmall.innerHTML = `This node has a total of ${selectedOption.getAttribute('totalram')} MB of RAM. It is currently reserving ${selectedOption.getAttribute('reservedram')} MB of RAM to other servers.`;
}