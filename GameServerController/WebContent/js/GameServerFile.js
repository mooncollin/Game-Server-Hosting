var fileInput = document.getElementById("fileUpload");
var zipInput = document.getElementById("zipUpload");
var form = document.getElementById("form");
var zipForm = document.getElementById("zipForm");
var folderField = document.getElementById("folderName");
var submitField = document.getElementById("submitFolder");
var fileListing = document.getElementById("fileListing");
var deleteMultipleSubmit = document.getElementById("deleteMultipleSubmit");
var fileAnchors = [];
var multipleDeleteOccuring = false;
var editOccuring = false;

for(let i = 0; i < fileListing.children.length; i++) {
	let row = fileListing.children[i];
	fileAnchors.push(row.children[0].cloneNode(true));
}

fileInput.addEventListener('change', function() {
	form.submit();
});
zipInput.addEventListener('change', function() {
	for(let i = 0; i < zipInput.files.length; i++) {
		if(!zipInput.files[i].name.endsWith(".zip")) {
			return;
		}
	}
	zipForm.submit();
});

function upload(type) {
	if(type === 'file') {
		fileInput.click();
	}
	else if(type === 'folder') {
		zipInput.click();
	}
}

function submitForm() {
	form.submit();
}

function submitFolder(url) {
	if(folderField.value != '') {
		window.location.href = url + "&newFolder=" + folderField.value; 
	}
}

function showFolderField() {
	if(folderField.hidden) {
		folderField.hidden = false;
		submitField.hidden = false;
		folderField.focus();	
	}
	else {
		folderField.hidden = true;
		submitField.hidden = true;
	}
}

function deleteFile(button) {
	if(confirm('Are you sure you want to delete "' + button.getAttribute('fileName') + '"?')) {
		window.location.href = button.getAttribute('location');
	}
}

function startDeleteMultiple(button) {
	if(!editOccuring) {
		button.innerHTML = '';
		let icon = document.createElement("i");
		if(!multipleDeleteOccuring) {
			deleteMultipleSubmit.hidden = false;
			multipleDeleteOccuring = true;
			icon.classList.add("fas", "fa-times");
			button.classList.remove("bg-light");
			button.classList.add("bg-primary");
			for(let i = 0; i < fileListing.children.length; i++) {
				let deleteCheckDiv = document.createElement("div");
				deleteCheckDiv.classList.add("list-group-item", "bg-dark", "text-light", "form-inline", "col-lg-11");
				let deleteCheck = document.createElement("input");
				deleteCheck.type = "checkbox";
				deleteCheck.classList.add("form-check-input");
				let deleteLabel = document.createElement("label");
				deleteLabel.classList.add('d-inline-block', 'w-25')
				let fileIcon = fileAnchors[i].children[0].cloneNode(true);
				let fileText = fileAnchors[i].children[1].cloneNode(true);
				deleteCheck.id = "delete-" + fileText.innerHTML;
				deleteLabel.setAttribute('for', deleteCheck.id);
				deleteLabel.appendChild(fileIcon);
				deleteLabel.appendChild(fileText);
				deleteCheckDiv.appendChild(deleteLabel.cloneNode(true));
				deleteCheckDiv.appendChild(deleteCheck.cloneNode(true));
				fileListing.children[i].replaceChild(deleteCheckDiv.cloneNode(true), fileListing.children[i].children[0]);
			}
		}
		else {
			deleteMultipleSubmit.hidden = true;
			multipleDeleteOccuring = false;
			icon.classList.add("fas", "fa-fire");
			button.classList.add("bg-light");
			button.classList.remove("bg-primary");
			for(let i = 0; i < fileAnchors.length; i++) {
				fileListing.children[i].replaceChild(fileAnchors[i].cloneNode(true), fileListing.children[i].children[0]);
			}
		}
		button.appendChild(icon.cloneNode(true));
	}
}

function submitDeleteMultiple(button) {
	if(multipleDeleteOccuring) {
		let checkedFiles = [];
		for(let i = 0; i < fileListing.children.length; i++) {
			let deleteCheckDiv = fileListing.children[i].children[0];
			let deleteCheck = deleteCheckDiv.children[1];
			if(deleteCheck.checked) {
				checkedFiles.push(deleteCheck.id.slice(7));
			}
		}
		if(checkedFiles.length > 0) {
			let confirmText = "Are you sure you want to delete these items?\n";
			for(let i = 0; i < checkedFiles.length; i++) {
				confirmText += "\n\t" + checkedFiles[i]; 
			}
			if(confirm(confirmText)) {
				let location = button.getAttribute('link') + checkedFiles.join(',');
				window.location.href = location;
			}
		}
	}
}

function editName(name, button) {
	if(!multipleDeleteOccuring) {
		let link = document.getElementById('file-' + name);
		let thisSpan = link.children[1];
		let icon = document.createElement('i');
		button.innerHTML = '';
		link.removeChild(thisSpan);
		let linkChildren = link.children;
		if(!editOccuring) {
			editOccuring = true;
			let nameInput = document.createElement('input');
			nameInput.type = 'text';
			nameInput.placeholder = 'Enter file name';
			nameInput.classList.add('form-control', 'w-25');
			nameInput.value = name;
			nameInput.onkeypress = function(event) {
				if(event.key === 'Enter' && nameInput.value != '' && nameInput.value != name) {
					console.log(button.getAttribute('location') + "&rename=" + nameInput.value);
					window.location.href = button.getAttribute('location') + "&rename=" + nameInput.value;
				}
			};
			
			let replacementTag = document.createElement('div');
			replacementTag.classList = link.classList;
			replacementTag.id = link.id;
			link.parentNode.replaceChild(replacementTag, link);
			for(let i = 0; i < linkChildren.length; i++) {
				replacementTag.append(linkChildren[i]);
			}
			
			replacementTag.append(nameInput);
			icon.classList.add('fas', 'fa-times');
			button.classList.remove('bg-warning');
			button.classList.add('bg-primary');
			replacementTag.href = 'javascript: void(0)';
			replacementTag.ondragstart = function() { return false; };
		}
		else {
			editOccuring = false;
			let span = document.createElement('span');
			span.innerHTML = name;
			button.classList.remove('bg-primary');
			button.classList.add('bg-warning');
			icon.classList.add('fas', 'fa-edit');
			
			let replacementTag = document.createElement('a');
			replacementTag.children = linkChildren;
			replacementTag.classList = link.classList;
			replacementTag.id = link.id;
			link.parentNode.replaceChild(replacementTag, link);
			for(let i = 0; i < linkChildren.length; i++) {
				replacementTag.append(linkChildren[i]);
			}
			
			replacementTag.append(span);
			replacementTag.href = button.getAttribute('linkLocation');
			replacementTag.ondragstart = function() { return true; };
		}
		button.append(icon.cloneNode(true));
	}
}