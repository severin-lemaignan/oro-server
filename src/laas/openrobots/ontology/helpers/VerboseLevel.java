package laas.openrobots.ontology.helpers;

public enum VerboseLevel {
	
	//Error that trigger the direct interuption of the application
	FATAL_ERROR, 
	
	//Error that "shouldn't happen" but the server can live with.
	SERIOUS_ERROR, 
	
	//"Normal" error, that will be reported to the clients
	ERROR, 
	
	//Not an error, but an important information that can alter the way the server works.
	WARNING, 
	
	//Important information that should be emphasized in a log
	IMPORTANT,
	
	//Emphasized standard information
	EMPHASIZE,
	
	//Standard information
	INFO,
	
	//Important debug info
	DEBUG,
	
	//Not very important debug info.
	VERBOSE;
}
