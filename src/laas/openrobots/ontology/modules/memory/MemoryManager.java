package laas.openrobots.ontology.modules.memory;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.helpers.VerboseLevel;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RSIterator;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.PropertyNotFoundException;

public class MemoryManager extends Thread {

	private final static int KNOWLEDGE_GARBAGE_COLLECTION_FREQ = 500; //in milliseconds
	
	private OntModel onto;
	private Set<String> watchedStmt;
	private boolean serverIsRunning = true;
	
	private Property p_createdOn;
	Property p_memoryProfile;
	
	public MemoryManager(OntModel model) {
		
		setName("Memory Manager"); //name the thread
		
		onto = model;
		watchedStmt = new HashSet<String>();
		

		p_createdOn = onto.createProperty(Namespaces.addDefault("stmtCreatedOn"));
		p_memoryProfile = onto.createProperty(Namespaces.addDefault("stmtMemoryProfile"));
	}
	
	@Override
	public void run() {
		
		Set<ReifiedStatement> stmtToRemove = new HashSet<ReifiedStatement>();
		
		while (serverIsRunning) {
			
			Date now = new Date();

			try {
				Thread.sleep(KNOWLEDGE_GARBAGE_COLLECTION_FREQ);
			} catch (InterruptedException e) {
				Logger.log("The memory manager thread has been interrupted!!\n", VerboseLevel.SERIOUS_ERROR);
				break;
			}
			
			//for (String rsName : watchedStmt) {
				
			onto.enterCriticalSection(Lock.READ);
				
			try {
				
				RSIterator rsIter = onto.listReifiedStatements() ;
		        while(rsIter.hasNext())
		        {
		            ReifiedStatement rs = rsIter.nextRS() ;
		            
		            try {

		            	String lexicalDate = rs.getRequiredProperty(p_createdOn).getLiteral().getLexicalForm();
		                
		                long elapsedTime = (now.getTime() - Helpers.getDateFromXSD(lexicalDate).getTime());
		                

		                MemoryProfile memProfile = MemoryProfile.fromString(rs.getRequiredProperty(p_memoryProfile).getString());
		                
		                if (elapsedTime > memProfile.duration())
		                {
		                	stmtToRemove.add(rs);
		                }
		            	
		            }
		            catch (PropertyNotFoundException pnfe)
		            {
		            //the reified statement	has no createdOn property. We skip it.
		            } catch (ParseException e) {
						Logger.log("The creation date of [" + Namespaces.toLightString(rs.getStatement()) + "] could not be parsed!\n", VerboseLevel.SERIOUS_ERROR);
					}
		        }
		        
		        
			} finally {
				onto.leaveCriticalSection();
			}
			
			onto.enterCriticalSection(Lock.WRITE);
			try {
				for (ReifiedStatement s : stmtToRemove) {
					Logger.log("Cleaning old statement [" + Namespaces.toLightString(s.getStatement()) +"].\n");
					s.getStatement().removeReification();
					s.getStatement().remove();
					s.removeProperties();					
				}
			}
			finally {
				onto.leaveCriticalSection();
			}
			stmtToRemove.clear();
		}
			
	}
	
	public void watch(String rsStmt) {
		watchedStmt.add(rsStmt);
	}
	
	@Override
	public void finalize() {
		serverIsRunning = false;
	}
	
}
