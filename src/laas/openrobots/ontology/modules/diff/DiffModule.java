package laas.openrobots.ontology.modules.diff;

import java.util.HashSet;
import java.util.Set;

import laas.openrobots.ontology.Helpers;
import laas.openrobots.ontology.IServiceProvider;
import laas.openrobots.ontology.Namespaces;
import laas.openrobots.ontology.RPCMethod;
import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.backends.OpenRobotsOntology.ResourceType;
import laas.openrobots.ontology.exceptions.NotComparableException;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.Lock;
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
public class DiffModule implements IServiceProvider {
	
	IOntologyBackend oro;
	
	Individual indivA;
	Individual indivB;
	OntModel modelA;
	OntModel modelB;
	Set<OntClass> typesA;
	Set<OntClass> typesB;
	
	boolean sameClass = false;
	
	Property type;
	
	public DiffModule(IOntologyBackend oro) {
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
	 * @see DiffModule Detailled explanations with a diagram.
	 */
	private Set<OntClass> commonAncestors(){
			
		if (sameClass) return indivA.listOntClasses(true).toSet();
		
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
		
		Set<OntClass> result = new HashSet<OntClass>(commonAncestors);
		
		for (OntClass c : commonAncestors) {
			
			if (result.size() == 1)
				return result;
			
			//We need to retrieve the class c IN the main ontology model to be
			//able to get the subclasses.
			OntClass classInOro = oro.getModel().getOntClass(c.getURI());
			
			assert(c.equals(classInOro));
			
			Set<OntClass> subClassesC = oro.getSubclassesOf(classInOro, false);
			
			subClassesC.retainAll(commonAncestors);
			
			if (!subClassesC.isEmpty())
				result.remove(c);				
			
		}

		return result;		
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
	 * @see DiffModule Detailled explanations with a diagram.
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

				
		Set<OntProperty> propA = modelA.listOntProperties().filterKeep(Namespaces.getDefaultNsFilter()).toSet();
		Set <OntProperty> commonProp = modelB.listOntProperties().filterKeep(Namespaces.getDefaultNsFilter()).toSet();
		commonProp.retainAll(propA);
		
		return commonProp;
	}
		
	/***************** DIFFERENCES *********************/

	/** Returns the computed differences between two concepts, relying on
	 * asserted and inferred fact in the ontology.
	 * 
	 * @see DiffModule Detailled explanations with a diagram.
	 * @see #getDifferences(String, String) Same "stringified" method. 
	 */
	public Set<Set<String>> getDifferences(OntResource conceptA, OntResource conceptB) throws NotComparableException {
		
		Set<Set<String>> result = new HashSet<Set<String>>();
		
		oro.getModel().enterCriticalSection(Lock.READ);
		
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
		
		oro.getModel().leaveCriticalSection();
		
		return result;
	}

	/** Returns the differences between two concepts (in their literal 
	 * representation).
	 * 
	 * @param conceptA The first concept to compare
	 * @param conceptB The second concept to compare
	 * @return the computed differences between two concepts as a set of sets of
	 *  stringified statements: for each different property (including type), a 
	 *  set of statement that explain for each concept its relation.
	 * @throws NotFoundException
	 * @throws NotComparableException
	 * @see DiffModule Detailled explanations with a diagram.
	 * @see #getDifferences(OntResource, OntResource)
	 */
	@RPCMethod(
			desc="given two concepts, return the list of relevant differences (types, properties...) between these concepts."
	)	
	public Set<Set<String>> getDifferences(String conceptA, String conceptB) throws NotFoundException, NotComparableException {
		
		oro.getModel().enterCriticalSection(Lock.READ);
			OntResource resA = oro.getModel().getOntResource(Namespaces.format(conceptA));
			OntResource resB = oro.getModel().getOntResource(Namespaces.format(conceptB));
		oro.getModel().leaveCriticalSection();
		
		if (resA == null) throw new NotFoundException("Concept " + conceptA + " doesn't exist in the ontology.");
		if (resB == null) throw new NotFoundException("Concept " + conceptB + " doesn't exist in the ontology.");
		
		return getDifferences(resA, resB);
	}
	
	
	/***************** SIMILARITIES *********************/
	
	/** Returns the computed similarities between two concepts, relying on
	 * asserted and inferred fact in the ontology.
	 * 
	 * @see DiffModule Detailled explanations with a diagram.
	 * @see #getSimilarities(String, String) Same "stringified" method.
	 */
	public Set<String> getSimilarities(OntResource conceptA, OntResource conceptB) throws NotComparableException {
		
		Set<String> result = new HashSet<String>();
		
		oro.getModel().enterCriticalSection(Lock.READ);
		
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
		
		oro.getModel().leaveCriticalSection();
		
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
	 * @see DiffModule Detailled explanations with a diagram.
	 * @see #getSimilarities(OntResource, OntResource)
	 */
	@RPCMethod(
			desc="given two concepts, return the list of relevant similarities (types, properties...) between these concepts."
	)	
	public Set<String> getSimilarities(String conceptA, String conceptB) throws NotFoundException, NotComparableException {
		
		oro.getModel().enterCriticalSection(Lock.READ);
			OntResource resA = oro.getModel().getOntResource(Namespaces.format(conceptA));
			OntResource resB = oro.getModel().getOntResource(Namespaces.format(conceptB));
		oro.getModel().leaveCriticalSection();
			
		if (resA == null) throw new NotFoundException("Concept " + conceptA + " doesn't exist in the ontology.");
		if (resB == null) throw new NotFoundException("Concept " + conceptB + " doesn't exist in the ontology.");
		
		return getSimilarities(resA, resB);
	}
}
