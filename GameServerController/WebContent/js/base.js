$(document).ready(function () {

    $('#sidebarCollapse').on('click', function () {
    	$('#sidebar').toggleClass('active');
    });

    $('.custom-file-input').on('change', function(e){
		var fileName = this.files[0].name;
		var nextSibling = e.target.nextElementSibling;
		nextSibling.innerText = fileName;
	});
});