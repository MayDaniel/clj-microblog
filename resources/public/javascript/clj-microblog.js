$(document).ready(function() {
		$("#dialog").dialog({
			modal: true,
      draggable: false,
      resizable: false,
			buttons: {
				Close: function() {
					$(this).dialog('close');
				}
			}
		});
   $("#slider").slider({
  step: 5,
  min: 0,
  max: 50,
  value: $("#hidden-num").val(),
  slide: function(event, ui) {
				$("#slider-val").val("Number of updates: " + ui.value);
			}
  });
  $("#slider-val").val("Number of updates: " + ($("#slider").slider("value")));
  $("input:submit").button();
  $("#search-tabs").tabs({ event: 'mouseover' });
});
