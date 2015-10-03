(function(){
	document.addEventListener("DOMContentLoaded", function(){
		var activeParams = [];
	    var selectElement = document.getElementById("method");
	    var paramDiv = document.getElementById("parameters");
	    var method = selectElement.value;
	    selectElement.addEventListener("change", selectChangeListenerFn);
	    selectChangeListenerFn();

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
		    	case "newflight":
		    		parameterList.push("id", "flightNumber", "numSeats");
		    		break;
		    	case "newcar":
		    		parameterList.push("id", "location", "numCars", "price");
		    		break;
		    	case "newroom":
		    		parameterList.push("id", "location", "numRooms", "price");
		    		break;
		    	case "newcustomer":
		    		parameterList.push("id");
		    		break;
		    	case "newcustomerid":
		    		parameterList.push("id", "customerNumber");
		    		break;
		    	case "deleteflight":
		    		parameterList.push("id", "flightNumber");
		    		break;
		    	case "deletecar":
		    		parameterList.push("id", "location");
		    		break;
		    	case "deleteroom":
		    		parameterList.push("id", "location");
		    		break;
		    	case "deletecustomer":
		    		parameterList.push("id", "customerNumber");
		    		break;
		    	case "queryflight":
		    		parameterList.push("id", "flightNumber");
		    		break;
		    	case "querycar":
		    		parameterList.push("id", "location");
		    		break;
		    	case "queryroom":
		    		parameterList.push("id", "location");
		    		break;
		    	case "querycustomer":
		    		parameterList.push("id", "customerNumber");
		    		break;
		    	case "queryflightprice":
		    		parameterList.push("id", "flightNumber");
		    		break;
		    	case "querycarprice":
		    		parameterList.push("id", "location");
		    		break;
		    	case "queryroomprice":
		    		parameterList.push("id", "location");
		    		break;
		    	case "reserveflight":
		    		parameterList.push("id", "customerNumber", "flightNumber");
		    		break;
		    	case "reservecar":
		    		parameterList.push("id", "customerNumber", "location");
		    		break;
		    	case "reserveroom":
		    		parameterList.push("id", "customerNumber", "location");
		    		break;
		    	case "itinerary":
		    		parameterList.push("id", "customerNumber", "location", "car", "room");
		    		break;
		    }
		    return parameterList;
	    }
	});
})();
