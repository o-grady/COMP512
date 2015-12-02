$(function(){
		var activeParams = [];
	    var selectElement = document.getElementById("method");
	    var paramDiv = document.getElementById("parameters");
	    var method = selectElement.value;
	    selectElement.addEventListener("change", selectChangeListenerFn);
	    selectChangeListenerFn();
    	var responseDiv = $('#responseDiv');
	    var form =  $('#requestForm');
	    form.submit(function(ev){
	    	responseDiv.height(200);
	    	responseDiv.html('Loading...');
	    	$.ajax({
	    		type: form.attr('method'),
	    		url: form.attr('action'),
	    		data: form.serialize(),
	    		dataType: "text"
	    	}).done(function(data){
	    			$('#responseDiv').html('<h2>Response received: '+ data.replace(/\n/g, '<br>') +'</h2>');
    		}).fail(function(data){
	    			$('#responseDiv').html('<h2>Error</h2>');
    		});
	    	ev.preventDefault();
	    });
	    function selectChangeListenerFn(){
	    	method = selectElement.value;
	    	var paramListStr = getParameterList(method);
	    	var paramListElement = paramListStr.map(function(parameter){
	    		 return document.getElementById(parameter);
	    	})
	    	displayParameters(paramListElement);

	    }
	    function displayParameters(parameterElementArray){
	    	activeParams.forEach(function(parameter){
	    		parameter.className += " formDiv"
	    	});
	    	if(parameterElementArray.length === 0){
	    		return;
	    	}
	    	parameterElementArray.reverse();
	    	parameterElementArray.forEach(function(parameter){
	    		var firstDiv = paramDiv.firstChild.nextSibling;
	    		parameter.className = parameter.className.replace( /(?:^|\s)formDiv(?!\S)/g , '' );
	    		paramDiv.insertBefore(parameter, firstDiv);
	    		activeParams.push(parameter);
	    	});
	    }
	    function getParameterList(method){
	    	var parameterList = [];
	    	switch(method){
		    	case "starttxn":
		    		break;
		    	case "commit":
		    		parameterList.push("transactionID");
		    		break;
		    	case "abort":
		    		parameterList.push("transactionID");
		    		break;
		    	case "newflight":
		    		parameterList.push("transactionID", "flightNumber", "numSeats", "price");
		    		break;
		    	case "newcar":
		    		parameterList.push("transactionID", "location", "numCars", "price");
		    		break;
		    	case "newroom":
		    		parameterList.push("transactionID", "location", "numRooms", "price");
		    		break;
		    	case "newcustomer":
		    		parameterList.push("transactionID");
		    		break;
		    	case "newcustomerid":
		    		parameterList.push("transactionID", "customerNumber");
		    		break;
		    	case "deleteflight":
		    		parameterList.push("transactionID", "flightNumber");
		    		break;
		    	case "deletecar":
		    		parameterList.push("transactionID", "location");
		    		break;
		    	case "deleteroom":
		    		parameterList.push("transactionID", "location");
		    		break;
		    	case "deletecustomer":
		    		parameterList.push("transactionID", "customerNumber");
		    		break;
		    	case "queryflight":
		    		parameterList.push("transactionID", "flightNumber");
		    		break;
		    	case "querycar":
		    		parameterList.push("transactionID", "location");
		    		break;
		    	case "queryroom":
		    		parameterList.push("transactionID", "location");
		    		break;
		    	case "querycustomer":
		    		parameterList.push("transactionID", "customerNumber");
		    		break;
		    	case "queryflightprice":
		    		parameterList.push("transactionID", "flightNumber");
		    		break;
		    	case "querycarprice":
		    		parameterList.push("transactionID", "location");
		    		break;
		    	case "queryroomprice":
		    		parameterList.push("transactionID", "location");
		    		break;
		    	case "reserveflight":
		    		parameterList.push("transactionID", "customerNumber", "flightNumber");
		    		break;
		    	case "reservecar":
		    		parameterList.push("transactionID", "customerNumber", "location");
		    		break;
		    	case "reserveroom":
		    		parameterList.push("transactionID", "customerNumber", "location");
		    		break;
		    	case "itinerary":
		    		parameterList.push("transactionID", "customerNumber", "flightNumbers", "location", "car", "room");

		    		break;
		    }
		    return parameterList;
	    }
});