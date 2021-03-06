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

package laas.openrobots.ontology.modules.categorization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.ResourceType;
import laas.openrobots.ontology.exceptions.NotComparableException;
import laas.openrobots.ontology.helpers.Helpers;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.Namespaces;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.service.IServiceProvider;
import laas.openrobots.ontology.service.RPCMethod;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;

/** The DiffModule computes differences and similarities between concepts.
 * 
 * <h2>Type comparison</h2>
 * Let's consider the following hierarchie:
 * <pre>
 *                  +-------+
 *                  | Thing |
 *                .'+-------._
 *             .-'            ``-._
 *      +----.'----+         +-----`-------+
 *      | Artifact |         | WhiteObject |
 *      +--.-----.-+         +------+--,---+
 *        /       `-.               /   \
 *  +---.'---+      +`-.--+        |     \
 *  | Animal |      | Car |        /      \
 *  +----+---+      +-----+       |        |
 *       \               `.      /        '.
 *        \                `-.  |          |
 *    +-----+-------+      +---`------+     |
 *    | WhiteAnimal |      | WhiteCar |      |
 *    +-------------+      +----------+     /
 *               `'--...__                ,'
 *                        ``---........,-'
 * </pre>
 * If you're looking for the similarities between concepts WhiteAnimal and
 * WhiteCar, the {@link #getSimilarities(OntResource, OntResource)} method would
 * return the set {Artifact, WhiteObject}, ie the set of classes that belong to
 * the intersection of the super-classes of both the concepts and without sub-
 * classes in this intersection (the common ancestors).
 * 
 * On the contrary, the {@link #getDifferences(OntResource, OntResource)} method
 * returns {Animal, Car} and {WhiteAnimal, WhiteCar}, ie the direct subclasses 
 * of the common ancestors that belongs to the hierarchies of the compared 
 * concepts.
 * 
 * <h3> Algo principle for common ancestors</h3>
 * 
 * <pre>
 * We want S2 such as:
 * S1 = superclasses(A) ∩ superclasses(B)
 * S2 = { c / c ∈ S1 AND subclasses(c) ∩ S1 = ∅ }
 * </pre>
 * 
 * In the example above, S2 = {Artifact, WhiteObject}
 * 
 * <h3>Algo principle for first different ancestors</h3>
 * 
 * <pre>
 * We want S3 such as:
 * S3 = ∪ { c / c ∈ S2,  
 * 		(directsubclasses(c) ∩ superclasses(A)) ∪ 
 * 		(directsubclasses(c) ∩ superclasses(B))
 * 		}
 * </pre>
 * 
 * In the example above, S3 = {{Animal, Car}, {WhiteAnimal, WhiteCar}}
 * 
 * The {@link #getDifferences(OntResource, OntResource)} method would actually
 * return a set of sets of statements, sorting by parent types the values for 
 * the two concepts (for instance, {@code [[a type Animal, b type Car], [a type
 * WhiteAnimal, b type WhiteCar]]}).
 * 
 * <h2>Comparison of other properties</h2>
 * 
 * The {@link #getSimilarities(OntResource, OntResource)} method will return as 
 * well the list of properties that are shared with the same object by both the
 * concepts.
 * 
 * The {@link #getDifferences(OntResource, OntResource)} returns on the contrary
 * properties shared by both concept but with different objects.
 * 
 * 
 * @author slemaign
 *
 * @since 0.6.4
 */
public class CategorizationModule implements IServiceProvider {
	
	private final String propertiesToRemove[] = {
			"http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
			"http://www.w3.org/2002/07/owl#sameAs", 
			"http://www.w3.org/2002/07/owl#differentFrom",
			"http://www.w3.org/2002/07/owl#topDataProperty",
			"http://www.w3.org/2002/07/owl#bottomDataProperty",
			"http://www.w3.org/2000/01/rdf-schema#label",
			"http://www.w3.org/2000/01/rdf-schema#comment"};

	private final String resourcesToRemove[] = {	
			"http://www.w3.org/2002/07/owl#Nothing"};

	IOntologyBackend oro;
	
	Individual indivA;
	Individual indivB;
	OntModel modelA;
	OntModel modelB;
	Set<OntClass> typesA;
	Set<OntClass> typesB;
	
	boolean sameClass = false;
	
	Property type;
	
	public CategorizationModule(IOntologyBackend oro) {
		super();
		this.oro = oro;
		type = oro.getModel().getProperty(Namespaces.format("rdf:type"));
		
		typesA = new HashSet<OntClass>();
		typesB = new HashSet<OntClass>();
	}
	
	private void loadModels(OntResource conceptA, OntResource conceptB) throws NotComparableException {
		
		if (Helpers.getType(conceptA) != Helpers.getType(conceptB)) throw new NotComparableException("Can not compare resources of different kind (a " + Helpers.getType(conceptA) + " and a " + Helpers.getType(conceptB) + " were provided).");
		
		//TODO: Implement a kind of comparison between resources different from individuals
		if (Helpers.getType(conceptA) != ResourceType.INSTANCE) throw new NotComparableException("Only the comparison between instances (individuals) is currently implemented");
		
		//We recreate two ontology models from details we get for each concepts.
		
		modelA = ModelFactory.createOntologyModel(oro.getModel().getSpecification(), oro.getSubmodel(conceptA));
		modelB = ModelFactory.createOntologyModel(oro.getModel().getSpecification(), oro.getSubmodel(conceptB));
		
		indivA = oro.getModel().getIndividual(conceptA.getURI());
		indivB = oro.getModel().getIndividual(conceptB.getURI());
		
		
		//get the list of types for concept A (filtering anonymous classes)
		typesA.clear();
		ExtendedIterator<OntClass> tmpA = indivA.listOntClasses(false);
		while (tmpA.hasNext()){
			OntClass c = tmpA.next();
			if (!c.isAnon())
				typesA.add(c);
		}
		
		//get the list of types for concept B (filtering anonymous classes)
		typesB.clear();
		ExtendedIterator<OntClass> tmpB = indivB.listOntClasses(false);
		
		while (tmpB.hasNext()){
			OntClass c = tmpB.next();
			if (!c.isAnon())
				typesB.add(c);
		}
		
		if (typesA.equals(typesB))
			sameClass = true;
		else
			sameClass = false;

	}
	
	/** Returns the classes that concept A and B have in common, as close as
	 * possible from concept A and B.
	 *
	 * 
	 * @return the closest common ancestors of concept A and concept B
	 * @see CategorizationModule Detailled explanations with a diagram.
	 */
	private Set<OntClass> commonAncestors(){
			
		//If both concept are from the same class, we simply return the parent
		//classes of one of the concept, only filtering anonymous classes.
		if (sameClass) {
			Set<OntClass> result = indivA.listOntClasses(true).toSet();
			
			Iterator<OntClass> it = result.iterator();
			
			while (it.hasNext())
				if (it.next().isAnon()) it.remove();
				
			return result;
		}
			
		//keep the intersection oftypes of A and B.
		Set<OntClass> commonAncestors = new HashSet<OntClass>(typesB);
		commonAncestors.retainAll(typesA);
		
		//commonAncestors shouldn't be empty: at least owl:Thing should be in 
		//common.
		assert(!commonAncestors.isEmpty());
		
		
		//**********************************************************************
		// Now, some heuristics to optimize the method in special cases.
		
		commonAncestors.remove(modelA.getResource(Namespaces.format("owl:Nothing")));
		
				
		if (commonAncestors.size() == 1) //owl:Thing
			return commonAncestors;
		
		commonAncestors.remove(modelA.getResource(Namespaces.format("owl:Thing")));
		
		
		//**********************************************************************
				
		Iterator<OntClass> it = commonAncestors.iterator();
		
		while (it.hasNext()) {
			OntClass c = it.next();
		
			//Removes anonymous classes
			if (c.isAnon()) {
				it.remove();
				continue;
			}
			
			//We need to retrieve the class c IN the main ontology model to be
			//able to get the subclasses.
			OntClass classInOro = oro.getModel().getOntClass(c.getURI());
			
			assert(c.equals(classInOro));
			
			Set<OntClass> subClassesC = oro.getSubclassesOf(classInOro, false);
			
			subClassesC.retainAll(commonAncestors);
			
			if (!subClassesC.isEmpty())
				it.remove();		
			
		}
		
		return commonAncestors;		
	}

	/** Returns the first different ancestors class of concept A and B.
	 * 
	 * * Algo principle for first different ancestors:
	 * 
	 * With S2 defined as in commonAncestor() method documentation,
	 * We want S4A and S4B such as:
	 * S3 = ∪ { c / c ∈ S2,  directsubclasses(c)}
	 * S4A = S3 ∩ superclasses(A)
	 * S4B = S3 ∩ superclasses(B)
	 * 
	 * @return The first differents ancestors of concept A and B.
	 * @see CategorizationModule Detailled explanations with a diagram.
	 */
	private Set<Set<Statement>> firstDifferentAncestors(){
		//**********************************************************************
		 
		Set<Set<Statement>> result = new HashSet<Set<Statement>>();

		if (sameClass) return result;
		
		
		for (OntClass c : commonAncestors()) {
			
			//We need to retrieve the class c IN the main ontology model to be
			//able to get the subclasses.
			OntClass classInOro = oro.getModel().getOntClass(c.getURI());
	
			assert(c.equals(classInOro));
			
			Set<OntClass> directSubClassesC = oro.getSubclassesOf(classInOro, true);
			
			//If c has no sub-classes, then it means that the two individuals 
			//have the same class. Skip it.
			if (directSubClassesC.isEmpty())
				continue;
			
			Set<OntClass> directSubClassesCbis = new HashSet<OntClass>(directSubClassesC);
					
			// Keep only ancestors of A that are direct subclasses of the current
			// common ancestors of both A and B.
			Set<Statement> ancestorsForC = new HashSet<Statement>();
			directSubClassesC.retainAll(typesA);
			
			
			if (directSubClassesC.isEmpty())
				directSubClassesC.add(c);
					
			for (OntClass dsc : directSubClassesC)
				ancestorsForC.add(oro.getModel().createStatement(indivA, type, dsc));
			
			
			//Idem for B
			directSubClassesCbis.retainAll(typesB);
			
			if (directSubClassesCbis.isEmpty())
				directSubClassesCbis.add(c);
			
			for (OntClass dsc : directSubClassesCbis)
				ancestorsForC.add(oro.getModel().createStatement(indivB, type, dsc));
			
			if (!ancestorsForC.isEmpty())
				result.add(ancestorsForC);
		}
				
		return result;
		
	}
	/*********************** Common properties*********************************/
	private Set<OntProperty> commonProperties() {

				
		//Set<OntProperty> propA = modelA.listOntProperties().filterKeep(Namespaces.getDefaultNsFilter()).toSet();
		ExtendedIterator<OntProperty> propTmpA = modelA.listOntProperties().filterKeep(Namespaces.getDefaultNsFilter());
		
		Set<OntProperty> propA = new HashSet<OntProperty>();
		while (propTmpA.hasNext()) {
			OntProperty p = propTmpA.next();
			propA.add(p);
		}
			
		Set <OntProperty> commonProp = modelB.listOntProperties().filterKeep(Namespaces.getDefaultNsFilter()).toSet();
		//commonProp.retainAll(propA);
		
		return commonProp;
	}
		
	/***************** DIFFERENCES *********************/

	/** Returns the computed differences between two concepts, relying on
	 * asserted and inferred fact in the ontology.
	 * 
	 * @param conceptA First concept to compare
	 * @param conceptB Second concept to compare
	 * @return Computed differences between two concepts as a set of sets of
	 *  stringified statements: for each different property (including type), a 
	 *  set of statement that explain for each concept its relation (for 
	 *  instance, {@code [[a type Animal, b type Car], [a type WhiteAnimal, b 
	 *  type WhiteCar]]})
	 * @see CategorizationModule Detailled explanations with a diagram.
	 * @see #getDifferences(String, String) Same "stringified" method. 
	 */
	public Set<Set<String>> getDifferences(OntResource conceptA, OntResource conceptB) throws NotComparableException {
		
		Set<Set<String>> result = new HashSet<Set<String>>();
		
		loadModels(conceptA, conceptB);
		
		for (Set<Statement> ss : firstDifferentAncestors()) {
			
			Set<String> resultForC = new HashSet<String>();
			
			for (Statement s : ss)
				resultForC.add(Namespaces.toLightString(s));
			
			result.add(resultForC);
		}
		
		//******************** Different properties ****************************
		for (OntProperty p : commonProperties()) {
			
			Set<String> resultForP = new HashSet<String>();
			
			//First build a set of all the object in A's model for the current
			//property.
			Set<RDFNode> objPropA = indivA.listPropertyValues(p).toSet();
						
			//Do the same for B.
			Set<RDFNode> objPropB = indivB.listPropertyValues(p).toSet();
			
			if (objPropA.equals(objPropB))
				continue;
			
			//Keep only the object of A that are not present for B
			Set<RDFNode> diffPropA = new HashSet<RDFNode>(objPropA); 
			diffPropA.removeAll(objPropB);
			
			assert(!diffPropA.isEmpty()); //shouldn't be empty since we are iterating of common properties
			
			//Keep only the object of B that are not present for A
			Set<RDFNode> diffPropB = new HashSet<RDFNode>(objPropB); 
			diffPropB.removeAll(objPropA);
			
			assert(!diffPropB.isEmpty()); //shouldn't be empty since we are iterating of common properties
			
			for (RDFNode o : diffPropA)
					resultForP.add(Namespaces.toLightString(indivA) + " " + Namespaces.toLightString(p) + " " + Namespaces.toLightString(o));

			for (RDFNode o : diffPropB)
				resultForP.add(Namespaces.toLightString(indivB) + " " + Namespaces.toLightString(p) + " " + Namespaces.toLightString(o));

			
			result.add(resultForP);
		}
		
		return result;
	}

	/** Returns the differences between two concepts (in their literal 
	 * representation).
	 * 
	 * @param conceptA The first concept to compare
	 * @param conceptB The second concept to compare
	 * @return computed differences between two concepts as a set of sets of
	 *  stringified statements: for each different property (including type), a 
	 *  set of statement that explain for each concept its relation.
	 * @throws NotFoundException
	 * @throws NotComparableException
	 * @see CategorizationModule Detailled explanations with a diagram.
	 * @see #getDifferences(OntResource, OntResource)
	 */
	@RPCMethod(
			category = "concept comparison",
			desc="given two concepts, return the list of relevant differences (types, properties...) between these concepts."
	)	
	public Set<Set<String>> getDifferences(String conceptA, String conceptB) throws NotFoundException, NotComparableException {
		
	
		OntResource resA = oro.getModel().getOntResource(Namespaces.format(conceptA));
		OntResource resB = oro.getModel().getOntResource(Namespaces.format(conceptB));
		
		if (resA == null) throw new NotFoundException("Concept " + conceptA + " doesn't exist in the ontology.");
		if (resB == null) throw new NotFoundException("Concept " + conceptB + " doesn't exist in the ontology.");
		
		return getDifferences(resA, resB);
	}
	
	/** Returns a list of properties that helps to differentiate individuals.
	 * 
	 * For instance, let consider the following ontology, with four instances 
	 * (plant1, animal1, animal2, animal3) :
	 * <pre>
	 *             +-------+
	 *             | Thing |
	 *             +--,'.--+
	 *             .-'   `-.
	 *           ,'         `.
	 *         ,'             `-.
	 *      .-'                  `.
	 *+----+--+                 +--+-----+
	 *| Plant |                 | Animal |
	 *+---+---+                 +--+++---+
	 *    |                   _,-"  |  `--.._
	 *    |                .-'      |        ``--..
	 *  plant1         animal1    animal2        animal3
	 *    |               |         / `.            \
	 *    |               |        /    `-.          \
	 * hasColor         eats     eats  hasColor   hasColor
	 *    |               |        |      |          |
	 *    |               |        |      |          |
	 *  green          banana    grass   blue       red
	 * </pre>
	 * 
	 * <p>
	 * Let's imagine the robot wants to answer an ambigious order like
	 * <i>Give me the thing!</i>: we need a way to disambiguate the different
	 * possible individuals in the {@code Thing} class.
	 * </p>
	 * 
	 * <p>
	 * The method return two things: 
	 * <ul>
	 * <li>a first set of properties that <b>totally discriminate</b> the set of 
	 * individual. It means that if we get the value of this property, we can 
	 * distinguish the individuals. Quite often, this set is empty because the
	 * set can not be split by a single property.</li>
	 * <li>a second set that contains the best property that can be 
	 * used to split the set of individuals (of the set of best properties if 
	 * several properties have the same efficiency). If the first set is not empty,
	 * the second set should obviously contain the same property/ies.</li>
	 * </ul>
	 * </p>
	 * 
	 * <p>
	 * The method proceeds as follows:
	 * <ol>
	 *  <li>Build a list of properties for each individuals, and sum their occurrence:
	 *<pre>  
	 *            p1        p2        p3
	 *-------------------------------------
	 *plant1:    {hasClass, hasColor}
	 *animal1:   {hasClass,           eats}
	 *animal2:   {hasClass, hasColor, eats}
	 *animal3:   {hasClass, hasColor}
	 *-------------------------------------
	 *      |p|=  4         3         2
	 *</pre></li>
	 *  <li>For each property, count the number of groups it forms (ie, the number
	 *  of distinct values) </li>
	 *  <pre>
	 *         | p1:hasClass | p2:hasColor | p3:eats
	 *---------+-------------+-------------+--------
	 *plant1   | Plant       | green       |
	 *animal1  | Animal      |             | banana
	 *animal2  | Animal      | blue        | grass
	 *animal3  | Animal      | red         |
	 *---------+-------------+-------------+--------
	 *|≠ elem.|= 2             3             2
	 *</pre>
	 *  <li>Remove properties that lead to only 1 group (ie, non selective
	 *  properties)</li>
	 *  <li>Return the property that has the highest occurrence, and then the best
	 *  selectivity (ie the biggest amount of groups). If several properties are
	 *  equal, return all of them.</li>
	 * </ol>
	 *   
	 * In our example, we would return {@code hasClass}.
	 * </p>
	 * <p>
	 * If we apply the method to the instances of the {@code Animal} class, it
	 * would return {@code {hasColor, eats}}.</p>
	 * 
	 * <p>
	 * It should be noted that this way of proceeding doesn't respect the OWL
	 * open world assumption: in OWA, the animal2 and the animal3 could possibly
	 * eat bananas as well, and thus, the {@code eats} property isn't selective.
	 * <br/>
	 * This rule is not enforced on purpose in this implementation: we stand on
	 * the viewpoint of a human-robot interaction where a robot only reason with
	 * what he positively knows. 
	 * </p>
	 * 
	 * @param individuals A set of concepts
	 * @return A list of two sets: first, a set of properties - if they exist -
	 * that are individually sufficient to differentiate all the individuals
	 * in the provided set from the values (objects) of these properties. Then a 
	 * set of properties that are sufficient OR NOT to distinguish the individuals.
	 * If they are sufficient, it's the same as the first set. Else, it's the most
	 * discriminating properties.
	 */
	//TODO WE DON'T HANDLE CORRECTLY NON-FUNCTIONNAL PROPERTIES!!
	//TODO Remove superproperties without direct values to solve several slowdown and bugs
	public List<Set<Property>> getDiscriminent(Set<OntResource> individuals) throws NotComparableException {

		
		Integer nbIndividuals = individuals.size();
		boolean isTotalDiscriminent = true;
		
		Set<Property> mostDiscriminantProperties = new HashSet<Property>();
				
		Map<Property, Integer> listOfProperties = new HashMap<Property, Integer>();
		Map<Property, Set<RDFNode>> listOfPropertiesValues = new HashMap<Property, Set<RDFNode>>();
		
		//Stores the list of properties for each individuals
		Map<OntResource, Set<Property>> listOfPropertiesByOwners = 
								new HashMap<OntResource, Set<Property>>();

		
		Logger.log("Running the getDiscriminent algorithm:\n",VerboseLevel.VERBOSE);
		
		//**********************************************************************
		//Build the list of properties with the number of individuals that use them
		// and the list of properties with the set of different values they have.
		
		for (OntResource i : individuals) {
			Map<Property, Set<RDFNode>> newProperties = getProperties(i);
			
			listOfPropertiesByOwners.put(i, newProperties.keySet());
			
			for(Property p : newProperties.keySet()) {
				if (listOfProperties.containsKey(p))
					listOfProperties.put(p, listOfProperties.get(p) + 1);
				else
					listOfProperties.put(p, 1);
				
				//We store objects that are different amongst all individuals
				if (!listOfPropertiesValues.containsKey(p))
					listOfPropertiesValues.put(p, new HashSet<RDFNode>());							
				listOfPropertiesValues.get(p).addAll(newProperties.get(p));

			}
		}

		if (Logger.verbosityMin(VerboseLevel.DEBUG)) {
			Logger.log("Property -> nb occurences:\n",VerboseLevel.DEBUG);
			for (Property p : listOfProperties.keySet())
				Logger.log(p + " -> " + listOfProperties.get(p) + "\n", VerboseLevel.DEBUG);
			
			Logger.cr();
			Logger.log("Property -> found values:\n",VerboseLevel.DEBUG);
			for (Property p : listOfPropertiesValues.keySet())
				Logger.log(p + " -> " + listOfPropertiesValues.get(p) + "\n", VerboseLevel.DEBUG);
			Logger.cr();
		}
		
		//**********************************************************************
		//Remove properties that lead to only 1 group (ie, non selective properties)
		//(only for properties shared by more than one individual)
		for (Property p : listOfPropertiesValues.keySet()) {
			if (listOfPropertiesValues.get(p).size() <= 1 && listOfProperties.get(p) > 1)
				listOfProperties.remove(p);
		}
	
		if (Logger.verbosityMin(VerboseLevel.DEBUG)) {
			Logger.log("Remaining properties after removal of non-selective ones:\n",VerboseLevel.DEBUG);
			for (Property p : listOfProperties.keySet())
				Logger.log(" -> " + p + "\n", VerboseLevel.DEBUG);
			Logger.cr();
		}
		
		//**********************************************************************
		//Sort the properties first by number of individuals that share this 
		//property, then by selectivity - ie, the number of different groups
		//the property can discriminate. Keep only the best.
		if (!listOfProperties.isEmpty()) {
			
			TreeMap<Integer, Set<Property>> normalizedListOfProperties = new TreeMap<Integer, Set<Property>>();
			
			for (Map.Entry<Property, Integer> e : listOfProperties.entrySet()) {
				Integer key = Math.min(e.getValue(), 
						listOfPropertiesValues.get(e.getKey()).size());
				
				if (!normalizedListOfProperties.containsKey(key))
					normalizedListOfProperties.put(key, new HashSet<Property>());
				
				normalizedListOfProperties.get(key).add(e.getKey());
			}
			
			if (Logger.verbosityMin(VerboseLevel.DEBUG)) {
				Logger.log("Normalized list of properties [min(nbOccurences, nbValues) -> Property]:\n",VerboseLevel.DEBUG);
				for (Integer i : normalizedListOfProperties.keySet())
					Logger.log(i + " -> " + normalizedListOfProperties.get(i) + "\n", VerboseLevel.DEBUG);
				Logger.cr();
			}
			
			//TreeMap<Integer, Set<Property>> listOfmostSharedProperties = 
			//	new TreeMap<Integer, Set<Property>>(Helpers.reverseMap(listOfProperties));
			
			//the last set of properties is the one shared by the max of individuals 
			Set<Property> mostSharedProperties = normalizedListOfProperties.lastEntry().getValue();
			
			//but, because of the superproperties, some properties may actually 
			//be shared by
			
			if (normalizedListOfProperties.lastKey() != nbIndividuals)
				isTotalDiscriminent = false;
			
			if (Logger.verbosityMin(VerboseLevel.DEBUG)) {
				Logger.log("Selected set of properties after sorting the most shared ones:\n",VerboseLevel.DEBUG);
				Logger.log(mostSharedProperties + "\n", VerboseLevel.DEBUG);
			}
			
			//If we have more than one property shared by the max of individual,
			//we select the subset that split the best the set of individuals. 
			if (mostSharedProperties.size() > 1) {
				
				TreeMap<Integer, Set<Property>> listOfMostDiscriminantProperty = new TreeMap<Integer, Set<Property>>();
				
				for (Property p : mostSharedProperties) {
						if (!listOfMostDiscriminantProperty.containsKey(
										listOfPropertiesValues.get(p).size()))
							listOfMostDiscriminantProperty.put(
										listOfPropertiesValues.get(p).size(), new HashSet<Property>());
						
						listOfMostDiscriminantProperty.get(listOfPropertiesValues.get(p).size()).add(p);
				}
				
				//we keep all properties that have the maximum n of different 
				//values.
				//From n = nb(Individuals) and over, we add all properties.
				//(n may be > nb(Individuals) for superproperties or 
				//non-functionnal properties.
				if (listOfMostDiscriminantProperty.lastEntry().getKey() <= 
						individuals.size())
					mostDiscriminantProperties = 
						listOfMostDiscriminantProperty.lastEntry().getValue();
				else
					for (Integer i : listOfMostDiscriminantProperty.keySet())					
						if (i >= individuals.size())
							mostDiscriminantProperties.addAll(listOfMostDiscriminantProperty.get(i));
				
				if (listOfMostDiscriminantProperty.lastKey() < nbIndividuals)
					isTotalDiscriminent = false;
			}
			//only one property is the most shared. We simply check if it 
			//completely split the set. 
			else 
			{
				for (Property p : mostSharedProperties)
					if (listOfPropertiesValues.get(p).size() != nbIndividuals)
						isTotalDiscriminent = false;
				
				mostDiscriminantProperties = mostSharedProperties;
			}
			
			if (Logger.verbosityMin(VerboseLevel.DEBUG)) {
				Logger.log("Final set of properties after sorting the " +
						"discriminating ones:\n",VerboseLevel.DEBUG);
				Logger.log(mostDiscriminantProperties + "\n", VerboseLevel.DEBUG);
				Logger.cr();
			}

		}
		
		//last filtering: if several properties are selected, we check that they
		//have no super-/sub-property relation.
		if (mostDiscriminantProperties.size() > 1) {
			
			//Find the resources involved in the remaining properties
			Set<OntResource> invovledIndividuals = new HashSet<OntResource>();
			for (Property p : mostDiscriminantProperties)
				for (OntResource i : listOfPropertiesByOwners.keySet())
					if (listOfPropertiesByOwners.get(i).contains(p))
						invovledIndividuals.add(i);
			
			Set<Property> listOfPropertiesToRemove = new HashSet<Property>(mostDiscriminantProperties);
			
			for (OntResource i : invovledIndividuals) {
				Set<Property> intersection = new HashSet<Property>(listOfPropertiesByOwners.get(i));
				intersection.retainAll(mostDiscriminantProperties);
				listOfPropertiesToRemove.retainAll(getRemovableSuperProperties(intersection));
			}
			
			Logger.log("List of super-properties that can be removed : \n", VerboseLevel.DEBUG);
			Logger.log(listOfPropertiesToRemove.isEmpty() ? "None\n" : (listOfPropertiesToRemove + "\n"), VerboseLevel.DEBUG);
			
			mostDiscriminantProperties.removeAll(listOfPropertiesToRemove);
			
		}

		Logger.log("Most discriminant properties: " + mostDiscriminantProperties
				+ (isTotalDiscriminent ? " (total discriminant)\n" : " (partial" +
						" discriminant)\n"), VerboseLevel.INFO);
		
		List<Set<Property>> res = new ArrayList<Set<Property>>();
		
		if (isTotalDiscriminent)
			res.add(mostDiscriminantProperties);
		else
			res.add(new HashSet<Property>());
		
		res.add(mostDiscriminantProperties);
		
		return res;

	}
	
	@RPCMethod(
			category = "concept comparison", 
			desc = "returns a list of properties that helps to differentiate individuals."
	)	
	public List<Set<String>> discriminate(Set<String> rawConcepts) 
							throws NotFoundException, NotComparableException {
		
		List<Set<String>> stringifiedRes = new ArrayList<Set<String>>();
		
		Set<OntResource> concepts = new HashSet<OntResource>();
		
			for (String s : rawConcepts) {
				OntResource r = oro.getModel().getOntResource(Namespaces.format(s));
				if (r == null) throw new NotFoundException("Concept " + s + " doesn't exist in the ontology.");
				concepts.add(r);
			}
		
		List<Set<Property>> res = getDiscriminent(concepts);
		
		for (Set<Property> sp : res) {
			Set<String> ss = new HashSet<String>();
			for (Property p : sp)
				ss.add(Namespaces.toLightString(p));
			stringifiedRes.add(ss);
		}			
		
		return stringifiedRes;
	}
	
	private Set<Property> getRemovableSuperProperties(Set<Property> propertiesList) {
		
		Set<Property> res = new HashSet<Property>();
		
		//Now, we want to mark properties that have subproperties in the set
		//for later filtering.
		//To do that, first we need to retrieve the OntProperty (ie, the Properties
		// attached to the model + reasonner)
		Set<OntProperty> listOfProperties = new HashSet<OntProperty>();
		for (Property p : propertiesList)
			listOfProperties.add(oro.createProperty(p.getURI()));
		
		//Then, for each OntProperty, we check if one of its subproperty also
		//belongs to the set. In this case, we discard it.
		for (OntProperty p : listOfProperties)
			for (OntProperty p2 : p.listSubProperties(true).toSet())
				if (listOfProperties.contains(p2)) {
					res.add(p);
					break;
				}
		
		return res;
	}
	
	private Map<Property, Set<RDFNode>> getProperties(OntResource c) {
		
		Map<Property, Set<RDFNode>> propertiesList = new HashMap<Property, Set<RDFNode>>();
		
		//Clean a bit the list of statements. We remove as well the type of the
		//resource. It will be added back a bit later.
		ExtendedIterator<Statement> stmtList = c.listProperties().filterKeep(
			new Filter<Statement>() {
	            @Override
				public boolean accept(Statement stmt) {
	                Property p = stmt.getPredicate();
	                RDFNode o = stmt.getObject();
	                
	                for (String pToRemove : propertiesToRemove) {
	                	if (p.getURI().equalsIgnoreCase(pToRemove))
	                			return false;
	                }
	                	
	                if (o.isURIResource()) {
	                	for (String rToRemove : resourcesToRemove) {
		                	if (o.as(Resource.class).getURI().equalsIgnoreCase(rToRemove))
		                			return false;
	                	}
	                }
	                	                
	                return true;
	            }
			});

		//lists all the properties for a given subject and add, for each property, the corresponding objects.
		while (stmtList.hasNext())
		{
						
			Statement tmp = stmtList.next();
			
			if(!propertiesList.containsKey(tmp.getPredicate()))
				propertiesList.put(tmp.getPredicate(), new HashSet<RDFNode>());
			
			propertiesList.get(tmp.getPredicate()).add(tmp.getObject());
			
			//System.out.println(tmp.getPredicate() + " -> " + tmp.getObject());
		}
		
		//Clean the properties list to remove all properties which have a 
		//subproperty with whom they share the same value.
		
		
		//First, we keep only properties that share a same value
		
		//we reverse the map 'properties->values' to retrieve the properties with the same value
		Map <RDFNode, Set<Property>> values = Helpers.reverseSetMap(propertiesList);
		for (RDFNode n : values.keySet()) {
			if (values.get(n).size() > 1) { //if more than one property for one value, then...
				Logger.log("Suspect properties: " + values.get(n) + ". " +
						"Is one of them a super-property without direct any value?\n", VerboseLevel.DEBUG);
				Set<OntProperty> suspectOntProperties = new HashSet<OntProperty>();
				
				//...we store these properties
				for (Property p : values.get(n)) {
					suspectOntProperties.add(oro.getModel().getOntResource(p).asProperty());
				}
				
				//and for each pair of them, we check if there a parent/child relation
				for (Property p : values.get(n)) {
					for (OntProperty op : suspectOntProperties) {
						
						if (!p.equals(op)) {
							OntProperty op2 = oro.getModel().getOntResource(p).asProperty(); 
							if(!op.hasEquivalentProperty(op2)) {
								if (op2.hasSubProperty(op, false)) {
									Logger.log("-> Removing " + Namespaces.toLightString(p) + " " + Namespaces.toLightString(n)+ " \n", VerboseLevel.DEBUG);
									propertiesList.get(p).remove(n);
									break;
								}
							}
						}
					}
				}
			}
				
		}
		
		//We add the type (class) of the resource.
		Set<RDFNode> cType = new HashSet<RDFNode>();
		cType.add(c.getRDFType(true));
		propertiesList.put(type, cType);
		
		return propertiesList;
	}

	
	/***************** SIMILARITIES *********************/
	
	/** Returns the computed similarities between two concepts, relying on
	 * asserted and inferred fact in the ontology.
	 * 
	 * Only <i>relevant</i> features are returned. For instance, only the closest
	 * common ancestor is returned, not all the shared hierarchy of two concepts.
	 * 
	 * @param conceptA First concept to compare
	 * @param conceptB Second concept to compare
	 * @return computed similarities between two concepts as a set of 
	 *  stringified partial statements: for each similar property (including 
	 *  type), a string of form "? prop obj" is returned.
	 * that are relevant common features of the two compared concepts.
	 * @see CategorizationModule Detailled explanations with a diagram.
	 * @see #getSimilarities(String, String) Same "stringified" method.
	 */
	public Set<String> getSimilarities(OntResource conceptA, OntResource conceptB) throws NotComparableException {
		
		Set<String> result = new HashSet<String>();
		
		loadModels(conceptA, conceptB);

		//*********************** Common ancestors *****************************
		for (OntClass c : commonAncestors()) {
			result.add("? rdf:type " + Namespaces.toLightString(c));
		}
				
		//*********************** Common properties ****************************
		for (OntProperty p : commonProperties()) {
			
			//First build a set of all the object in A's model for the current
			//property.
			Set<RDFNode> objPropA = indivA.listPropertyValues(p).toSet();
						
			//Do the same for B and keep only the common ones.
			Set<RDFNode> objPropB = indivB.listPropertyValues(p).toSet();
			
			objPropB.retainAll(objPropA);
			
			for (RDFNode o : objPropB)
					result.add("? " + Namespaces.toLightString(p) + " " + Namespaces.toLightString(o));
		}
		
		return result;
	}
	
	/** Returns the similarities between two concepts (in their literal 
	 * representation).
	 * 
	 * @param conceptA The first concept to compare
	 * @param conceptB The second concept to compare
	 * @return the computed similarities between two concepts as a set of 
	 *  stringified partial statements: for each similar property (including 
	 *  type), a string of form "? prop obj" is returned.
	 * @throws NotFoundException
	 * @throws NotComparableException
	 * @see CategorizationModule Detailled explanations with a diagram.
	 * @see #getSimilarities(OntResource, OntResource)
	 */
	@RPCMethod(
			category = "concept comparison", 
			desc = "given two concepts, return the list of relevant similarities (types, properties...) between these concepts."
	)	
	public Set<String> getSimilarities(String conceptA, String conceptB) throws NotFoundException, NotComparableException {
		
		OntResource resA = oro.getModel().getOntResource(Namespaces.format(conceptA));
		OntResource resB = oro.getModel().getOntResource(Namespaces.format(conceptB));
			
		if (resA == null) throw new NotFoundException("Concept " + conceptA + " doesn't exist in the ontology.");
		if (resB == null) throw new NotFoundException("Concept " + conceptB + " doesn't exist in the ontology.");
		
		return getSimilarities(resA, resB);
	}
	
	/***************** CATEGORIES *********************/
	
	/**
	 * Try to build a categories from a set of individuals.
	 * 
	 * This method does a fair amount of processing to compute possible categories
	 * in a set of individuals :
	 * <ol>
	 * 	<li></li>
	 *  <li></li>
	 * </ol>
	 */
	public Map<OntClass, Set<Individual>> makeCategories(Set<OntResource> resources) throws NotComparableException {
		return null;	
	}
	
}
