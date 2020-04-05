var fields = document.getElementsByClassName("form-control");
var startingFieldValues = [];
for(let i = 0; i < fields.length; i++) {
	if(fields[i].type === "checkbox") {
		startingFieldValues.push(fields[i].checked);
	}
	else {
		startingFieldValues.push(fields[i].value);
	}
}
var firstListItem = document.getElementsByClassName("list-group-item")[0];
var editButton = document.getElementById("editButton");
var submitButton = document.getElementById("submitButton");
var settingsForm = document.getElementById("settingsForm");
var editMode = false;

function edit() {
	for(let i = 0; i < fields.length; i++) {
		fields[i].disabled = editMode;
	}
	submitButton.hidden = editMode;
	editMode = !editMode;
	let icon = document.createElement("i");
	editButton.innerHTML = "";
	if(editMode) {
		icon.classList.add("fas", "fa-times");
		editButton.classList.remove("btn-warning");
		editButton.classList.add("btn-primary");
	}
	else {
		icon.classList.add("fas", "fa-pencil-alt");
		editButton.classList.remove("btn-primary");
		editButton.classList.add("btn-warning");
		for(let i = 0; i < fields.length; i++) {
			if(fields[i].type === "checkbox") {
				fields[i].checked = startingFieldValues[i];
			}
			else {
				fields[i].value = startingFieldValues[i];
			}
		}
	}
	editButton.appendChild(icon);
}