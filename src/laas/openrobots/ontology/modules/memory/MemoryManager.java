/*
 * Copyright (c) 2008-2010 LAAS-CNRS Séverin Lemaignan slemaign@laas.fr
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/

package laas.openrobots.ontology.modules.memory;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.mindswap.pellet.jena.PelletInfGraph;

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

	private final static int KNOWLEDGE_GARBAGE_COLLECTION_FREQ = 1000; //in milliseconds
	
	private OntModel onto;
	private Set<String> watchedStmt;
	private boolean serverIsRunning = true;
	
	private Property p_createdOn;
	Property p_memoryProfile;
	
	public MemoryManager(OntModel model) {
		
		setName("Agent memory manager"); //name the thread
		
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
			
			onto.enterCriticalSection(Lock.READ);
			Logger.logConcurrency(Logger.LockType.ACQUIRE_READ, "MemoryManager1");
				
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
				Logger.logConcurrency(Logger.LockType.RELEASE_READ, "MemoryManager1");
			}
			
			if (!stmtToRemove.isEmpty()) {
				onto.enterCriticalSection(Lock.WRITE);
				Logger.logConcurrency(Logger.LockType.ACQUIRE_WRITE, "MemoryManager2");
				try {
					for (ReifiedStatement s : stmtToRemove) {
						Logger.log("Cleaning old statement [" + Namespaces.toLightString(s.getStatement()) +"].\n");
						s.getStatement().removeReification();
						s.getStatement().remove();
						s.removeProperties();					
					}
				}
				finally {
					/* Pellet is not thread-safe. To avoid bad concurrency issue, we lock the
					model and classify it before each query.
					Cf http://clarkparsia.com/pellet/faq/jena-concurrency/ for details.
					*/			
					((PelletInfGraph) onto.getGraph()).classify();
					
					onto.leaveCriticalSection();
					Logger.logConcurrency(Logger.LockType.RELEASE_WRITE, "MemoryManager2");
				}
				stmtToRemove.clear();
			}
		}
			
	}
	
	public void watch(String rsStmt) {
		watchedStmt.add(rsStmt);
	}
	
	public void close() {
		serverIsRunning = false;
	}
	
}
