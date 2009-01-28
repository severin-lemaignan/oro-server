/*
 * ©LAAS-CNRS (2008-2009)
 * 
 * contributor(s) : Séverin Lemaignan <severin.lemaignan@laas.fr>
 * 
 * This software is a computer program whose purpose is to interface
 * with an ontology in a robotics context.
 * 
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 * 
*/

package laas.openrobots.ontology;

import java.util.Dictionary;
import java.util.Hashtable;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class Namespaces {
	
	/**
	 * Standard OWL namespace ({@value})
	 */
	public static final String owl_ns = "http://www.w3.org/2002/07/owl#";
	/**
	 * Standard RDF namespace ({@value})
	 */
	public static final String rdf_ns = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	/**
	 * Standard RDFS namespace ({@value})
	 */
	public static final String rdfs_ns = "http://www.w3.org/2000/01/rdf-schema#";

	/**
	 * Standard XML Schema namespace ({@value})
	 */
	public static final String xsd_ns = "http://www.w3.org/2001/XMLSchema#";
	
	/**
	 * The LAAS OpenRobots ontology namespace ({@value})
	 */
	public static final String oro_ns = "http://www.laas.fr/~slemaign/onto/openrobots.owl#";
	
	/**
	 * The default namespace. This static field is set up at runtime from the configuration file.
	 */
	public static String DEFAULT_NS = "";
	
	private static Hashtable<String, String> namespaces;
	
	static
	{
		namespaces = new Hashtable<String, String>();
		namespaces.put("oro", oro_ns);
		namespaces.put("owl", owl_ns);
		namespaces.put("rdf", rdf_ns);
		namespaces.put("rdfs", rdfs_ns);
		namespaces.put("xsd", xsd_ns);
	}

	/**
	 * Returns a list of commons namespace prefixes (currently, OWL, RDF, RDFS, XSD, LAAS OpenRobots), in SPARQL format, to be included in SPARQL queries.
	 * @return The {@code PREFIX} part of a SPARQL request. 
	 */
	public static String prefixes()
	{
		return 
		"PREFIX owl: <" + owl_ns + "> \n" +
		"PREFIX xsd: <" + xsd_ns + "> \n" +
		"PREFIX rdf: <" + rdf_ns + "> \n" +
		"PREFIX rdfs: <" + rdfs_ns + "> \n" +
		"PREFIX oro: <"+ oro_ns +"> \n"; //OpenRobots namespace.
	}
	
	/**
	 * Convert namespace shortcut in their expanded form (which includes a trailing {@code #}).<br/>
	 * For instance:<br/>
	 * {@code Namespaces.getNamespace("oro")} returns {@code http://www.owl-ontologies.com/openrobots.owl#}
	 * @param ns One of the known namespace shortcut ("owl" for OWL namespace, "rdf" and "rdfs" for RDF, "xsd" for XML Schema, "oro" for OpenRobots)
	 * @return The expanded namespace or, if the shortcut is not known, the shortcut itself, followed by ":"
	 * @see #getPrefix(String)
	 */
	public static String getNamespace(final String ns){
		if (namespaces.containsKey(ns)) return namespaces.get(ns);
		else return ns + ":";
	}

	/**
	 * Convert expanded namespace to their shortcut (prefix) followed by ":" or to nothing if the namespace is the default namespace.<br/>
	 * For instance:<br/>
	 * {@code Namespaces.getPrefix("http://www.owl-ontologies.com/openrobots.owl#")} returns {@code oro}
	 * @param ns One of the known namespace (ending with "#"). 
	 * @return The corresponding namespace prefix suffixed with ":", or nothing if the namespace is the default namespace, or, if the namespace is not known, the namespace itself.
	 * @see #getNamespace(String)
	 */
	public static String getPrefix(final String ns){
		
		if (ns.compareTo(DEFAULT_NS) == 0) return "";
		
		if (namespaces.containsValue(ns))
		{
			for(String prefix: namespaces.keySet()){
		        if(namespaces.get(prefix).equals(ns)) {
		            return prefix + ":";
		        }
		    }			
		}
		
		return ns;
	}

	
	/**
	 * Replace the namespace prefix with its expanded form, when known.<br/>
	 * For instance, {@code "xsd:boolean"} will be returned as {@code "http://www.w3.org/2001/XMLSchema#boolean"}.<br/>
	 * If the URI doesn't contain any prefix or if the prefix is unknow, the URI is returned as it.
	 * 
	 * It can be used as well to expand prefixes in string representation of literals ({@code "true^^xsd:boolean"} will be transformed into {@code true^^http://www.w3.org/2001/XMLSchema#boolean"}
	 * 
	 * @param uri The resource URI with (or without) a namespace prefix, or the string representation of a literal.
	 * @return The URI (or literal) with an expanded namespace.
	 */
	public static String expand(final String uri)
	{
		int step1 = uri.indexOf("^^");
		int step2 = uri.indexOf(":");
		
		if (step2==-1)
			return uri;		
		if (step1==-1)
			return getNamespace(uri.substring(0, step2)) + uri.substring(step2 + 1);
		
		return uri.substring(0,step1+2) + expand(uri.substring(step1+2));
	}
	
	/**
	 * Try to replace a complete namespace by its prefix (if known) or remove the namespace if it's the default one. This method does the opposite to {@link #expand(String)}.<br/>
	 * If the prefix for the namespace is unknown, complete URI is returned.
	 * 
	 * @param uri The resource to output as a string.
	 * @return The literal representation of the resource, with an short namespace (or no namespace if it is the default one).
	 */
	public static String contract(String uri)
	{
		int step1 = uri.indexOf("^^");
		int step2 = uri.indexOf("#");
		
		if (step2 == -1) return uri; //no expanded namespace
		
		if (step1 == -1) return getPrefix(uri.substring(0, step2 + 1)) + uri.substring(step2 + 1);
		
		return uri.substring(0,step1+2) + contract(uri.substring(step1+2));
		

	}
	
	/**
	 * Try to replace a complete namespace by its prefix (if known) or remove the namespace if it's the default one. This method does the opposite to {@link #expand(String)}.<br/>
	 * If the prefix for the namespace is unknown, complete URI is returned.
	 * 
	 * @param res The RDFNode which is to output as a string.
	 * @return The literal representation of the resource, with short namespace (or no namespace if it is the default one).
	 */
	public static String toLightString(final RDFNode res)
	{
		if (res.isResource()) return contract(((Resource)res).getURI());
		
		if (res.isLiteral())
		{
			Literal lit = (Literal)res;
			if (lit.getDatatype() == XSDDatatype.XSDdouble ||
				lit.getDatatype() == XSDDatatype.XSDint ||
				lit.getDatatype() == XSDDatatype.XSDboolean) return lit.getLexicalForm();
			
			return "'" + lit.getLexicalForm() + "'^^" + contract((lit).getDatatypeURI());
		}
		
		return res.toString();
	}

	/**
	 * Applies {@link #toLightString(RDFNode)} to each members of a statement.<br/>
	 * 
	 * @param stmt The statement to output as a string.
	 * @return The literal representation of the statement, with short namespaces (or no namespace if it is the default one).
	 */
	public static String toLightString(Statement stmt) {
		return toLightString(stmt.getSubject()) + " " + toLightString(stmt.getPredicate()) + " " + toLightString(stmt.getObject());
	
	}
	
	/**
	 * Add the default namespace to a resource.<br/>
	 * This method is really naive: it will prefix a string with the default namespace if the string does not contain the characters ":" or "#".<br/>
	 * If the URI already contains a prefix, the URI is returned as it.
	 * 
	 * @param resource The resource URI with (or without) a namespace prefix, or the string representation of a literal.
	 * @return The resource prefixed with the default namespace.
	 * @see #DEFAULT_NS
	 * @see #setDefault(String)
	 */
	public static String addDefault(final String resource)
	{
		int index = resource.indexOf(":");
		int index2 = resource.indexOf("#");
		
		if (index == -1 && index2 == -1)
			{return DEFAULT_NS + resource;}
		else
			{return resource;}
	}
	
	/**
	 * Formats a resource's namespace, adding the default one if none is specified.<br/>
	 * This method is actually a simple shortcut for a call to both {@link #addDefault(String)} and {@link #expand(String)}.
	 * 
	 * @param resource A string representating a resource (its lexical form).  
	 * @return The formatted namespace.
	 * @see #addDefault(String)
	 * @see #expand(String)
	 */
	public static String format(final String resource)
	{
		//TODO would be nice to throw an exception if the namespace is not valid...
		return addDefault(expand(resource));
	}
	
	/**
	 * Set the default namespace.<br/>
	 * This method is meant to be called at least once at application startup, to define a default namespace (from a configuration file, for instance).
	 * 
	 * @param defaultNS The expanded namespace.
	 * @see #addDefault(String)
	 * @see #DEFAULT_NS
	 */
	public static void setDefault(final String defaultNS)
	{
		if(defaultNS != null) DEFAULT_NS = defaultNS;
	}
	

}
