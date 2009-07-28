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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import laas.openrobots.ontology.exceptions.IllegalStatementException;

import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;

/**
 * This class provides various static helper methods for some common parsing and formatting tasks.
 * @author slemaign
 *
 */
public class Helpers {
	/**
	 * Parse a SPARQL string representing a literal to an actual Jena {@link com.hp.hpl.jena.rdf.model.Literal}. The method actually returns a {@link com.hp.hpl.jena.rdf.model.RDFNode} because if the literal is not recognized, it falls back on a generic RDFNode. To be sure the result is actually a literal, the {@link com.hp.hpl.jena.rdf.model.RDFNode#isLiteral()} method can be used.<br/> 
	 * @param lex the string representing the literal.
	 * @param model the model linked to the ontology in which the literal is to be created
	 * @return a RDFNode holding the literal
	 * @throws IllegalStatementException
	 * @see laas.openrobots.ontology.backends.IOntologyBackend#createStatement(java.lang.String) Details regarding the syntax of literals. 
	 * @see #literalToSparqlSyntax(Literal)
	 */
	public static RDFNode parseLiteral(final String lex, ModelCom model) throws IllegalStatementException
	{
		int separatorIndex = lex.indexOf("^^");
		
		//handle the case of
		if (separatorIndex == -1)
		{
			
			if (lex.contentEquals("true")) return model.createTypedLiteral(true);
			if (lex.contentEquals("false")) return model.createTypedLiteral(false);
			
			//lex is "*" or '*' -> create a string literal
			if ((lex.startsWith("\"") && lex.endsWith("\"")) || (lex.startsWith("'") && lex.endsWith("'"))) return model.createTypedLiteral(lex.substring(1, lex.length()-1), XSDDatatype.XSDstring);
			
			try {
				Double.parseDouble(lex);
				if (!lex.contains(".")) //assume it's an integer
					return model.createTypedLiteral(lex, XSDDatatype.XSDint);
				else //assume it's a double
					return model.createTypedLiteral(lex, XSDDatatype.XSDdouble);
			} catch (NumberFormatException e) {
				//assume it's a plain object
				return model.getResource(Namespaces.format(lex));				
			}
		}
		else				
		{
			String lexType[] = lex.split("\\^\\^");
				if ((lexType[0].startsWith("\"") && lexType[0].endsWith("\"")) || (lexType[0].startsWith("'") && lexType[0].endsWith("'"))) lexType[0] = lexType[0].substring(1, lexType[0].length()-1); //a not-so-clean way to remove quotes around the literal.
				if (lexType[1].startsWith("<") && lexType[1].endsWith(">")) lexType[1] = lexType[1].substring(1, lexType[1].length()-1); //a not-so-clean way to remove < and > around the datatype.
				Literal object;
				try {
					object = model.createTypedLiteral(lexType[0], Namespaces.expand(lexType[1]));
					if (object.getValue() == null) throw new IllegalStatementException("Invalid XML datatype! (was: " + lex + ").");
					
				} catch (DatatypeFormatException e) {
					throw new IllegalStatementException("Lexical form doesn't match datatype! (was: " + lex + ").");
				}
	
				
				
				return object;
				
		}
		
	}
	
	/**
	 * Formats a literal to a SPARQL-compatible string.
	 * @param lit a literal to be formatted
	 * @return a string representing the literal with SPARQL syntax.
	 * @see #parseLiteral(String, ModelCom)
	 */
	public static String literalToSparqlSyntax(Literal lit)
	{
		RDFDatatype litClass = lit.getDatatype();
		
		if (litClass == XSDDatatype.XSDboolean || litClass == XSDDatatype.XSDint || litClass == XSDDatatype.XSDdouble)
		{
			return lit.getLexicalForm();
		}
			
		else return "'" + lit.getLexicalForm() + "'^^<" + lit.getDatatypeURI() + ">";		
	}

	/**
	 * Returns a Java Date object from its XML Schema Dataype (XSD) representation in the GMT timezone.
	 * 
	 * Note: the reverse operation (Date to XSD) can be achieved by {@code OntModel.createTypedLiteral(Calendar.getInstance())}.
	 * 
	 * @param xsdDateTime a XSD formatted date
	 * @return the corresponding Java Date object.
	 * @throws ParseException
	 */
	public static Date getDateFromXSD(String xsdDateTime) throws ParseException {
	    SimpleDateFormat ISO8601Local = new SimpleDateFormat(
	      "yyyy-MM-dd'T'HH:mm:ss");
	    TimeZone timeZone = TimeZone.getTimeZone("GMT");
	    ISO8601Local.setTimeZone(timeZone);

		return ISO8601Local.parse(xsdDateTime);
	}
}
