package laas.openrobots.ontology.memory;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import laas.openrobots.ontology.Helpers;
import laas.openrobots.ontology.Namespaces;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RSIterator;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.Statement;
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
	
	public void run() {
		
		Set<ReifiedStatement> stmtToRemove = new HashSet<ReifiedStatement>();
		
		while (serverIsRunning) {
			
			Date now = new Date();

			try {
				Thread.sleep(KNOWLEDGE_GARBAGE_COLLECTION_FREQ);
			} catch (InterruptedException e) {
				System.err.println("The memory manager thread has been interrupted!!");
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
						System.err.println("The creation date of [" + rs.getStatement() + "] could not be parsed!");
					}
		        }
		        
		        
			} finally {
				onto.leaveCriticalSection();
			}
			
			onto.enterCriticalSection(Lock.WRITE);
			try {
				for (ReifiedStatement s : stmtToRemove) {
					System.out.println(" * Cleaning old statement [" + s.getStatement() +"].");
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
	
	public void finalize() {
		serverIsRunning = false;
	}
	
}
