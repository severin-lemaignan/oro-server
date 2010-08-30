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

package laas.openrobots.ontology.helpers;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;

import laas.openrobots.ontology.OroServer;
import laas.openrobots.ontology.PartialStatement;
import laas.openrobots.ontology.backends.ResourceType;
import laas.openrobots.ontology.exceptions.IllegalStatementException;
import laas.openrobots.ontology.exceptions.OntologyServerException;

import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;

/**
 * This class provides various static helper methods for some common parsing and formatting tasks.
 * @author slemaign
 *
 */
public class Helpers {
	
	static Random handOfGod;
	
	static {
		handOfGod = new Random(System.currentTimeMillis());
	}
	
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
	
	/**Formats a statement for inclusion in a SPARQL query.</br>
	 * 
	 * @return the formatted statement.
	 * @see PartialStatement#asSparqlRow() asSparqlRow() in PartialStatement class
	 */
	public static String asSparqlRow(Statement stmt){	
		return 	"<" + stmt.getSubject().toString() + "> " + 
				"<" + stmt.getPredicate().toString() + "> " +
				(stmt.getObject().isLiteral()? 
								literalToSparqlSyntax((Literal)stmt.getObject()) : 
								"<" + stmt.getObject().toString() + ">")
				+ " .\n";
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

	public static String getLabel(OntResource resource) {
		return getLabel(resource, OroServer.DEFAULT_LANGUAGE); 
	}
	
	public static String getLabel(OntResource resource, String languageCode) {
		return ((resource.getLabel(languageCode) == null) ? 
					((resource.getLabel(null) == null) ? resource.getLocalName() : resource.getLabel(null)) : 
					resource.getLabel(languageCode)); 
	}
	
	public static String getId(OntResource resource) {
		return Namespaces.contract(resource.getURI());
		/*
		try {
			return URLEncoder.encode(resource.getURI(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Logger.log("This plateform doesn't support UTF-8 encoding! Exiting.", VerboseLevel.FATAL_ERROR);
			System.exit(1);
			return "";
		}
		*/

	}
	
		
	public static ResourceType getType(OntResource resource) {
		ResourceType type;

		if (resource == null) return ResourceType.UNDEFINED;
		
		try {
			
			if (resource.isClass() && resource.isIndividual()) {
				Logger.log("Impossible to determine the type of the resource." +
						"Maybe you are using a OWL-Full reasonner or you disabled " +
						"the strict mode.\n", VerboseLevel.SERIOUS_ERROR);
						return ResourceType.UNDEFINED;
			}
			
			if (resource.isClass())
				type = ResourceType.CLASS;
			else if (resource.isIndividual())
				type = ResourceType.INSTANCE;
			else if (resource.isDatatypeProperty())
				type = ResourceType.DATATYPE_PROPERTY;
			else if (resource.isProperty())
				type = ResourceType.OBJECT_PROPERTY;
			else
				type = ResourceType.UNDEFINED;
			
		} catch (NoSuchFieldError nsfe) {
			Logger.log("Couln't determine the type of " + resource + "! Issue with the reasonner?", VerboseLevel.SERIOUS_ERROR);
			type = ResourceType.UNDEFINED;
		}
		
		return type;
	}
	
	/** Split a string into tokens separated by commas. It properly handle quoted strings and arrays delimited by [] or {}.
	 * 
	 * @param str A string to tokenize.
	 * @return A list of tokens
	 */
	public static List<String> tokenize (String str, char delimiter) {
		ArrayList<String> tokens = new ArrayList<String>();
		
		int countSBrackets = 0;
		int countCBraces = 0;
		int countSQuotes = 0;
		boolean inSQuotes = false;
		int countDQuotes = 0;
		boolean inDQuotes = false;
		
		int start_pos = 0;
		
		for (int i = 0; i < str.length() ; i++) {
			if (str.charAt(i) == '[' && ((i > 0) ? str.charAt(i - 1) != '\\' : true))
				countSBrackets++;
			if (str.charAt(i) == ']' && ((i > 0) ? str.charAt(i - 1) != '\\' : true))
				countSBrackets--;
			
			if (str.charAt(i) == '{' && ((i > 0) ? str.charAt(i - 1) != '\\' : true))
				countCBraces++;
			if (str.charAt(i) == '}' && ((i > 0) ? str.charAt(i - 1) != '\\' : true))
				countCBraces--;
			
			if (str.charAt(i) == '"' && ((i > 0) ? str.charAt(i - 1) != '\\' : true) && !inDQuotes) {countDQuotes++; inDQuotes = true;}
			else if (str.charAt(i) == '"' && ((i > 0) ? str.charAt(i - 1) != '\\' : true) && inDQuotes) {countDQuotes--; inDQuotes = false;}
			
			if (str.charAt(i) == '\'' && ((i > 0) ? str.charAt(i - 1) != '\\' : true) && !inSQuotes) {countSQuotes++; inSQuotes = true;}
			else if (str.charAt(i) == '\'' && ((i > 0) ? str.charAt(i - 1) != '\\' : true) && inSQuotes) {countSQuotes--; inSQuotes = false;}
			
			if (str.charAt(i) == delimiter &&
					countSBrackets == 0 &&
					countCBraces == 0 &&
					countSQuotes == 0 &&
					countDQuotes == 0) {
				tokens.add(str.substring(start_pos, i));
				start_pos = i + 1;
			}
		}
		
		tokens.add(str.substring(start_pos, str.length()));

		return tokens;
		
	}
	
	/**
	 * This static method takes a map and return the list of the key sorted by
	 * their values in ascending order.
	 * @param <K> The type of the map keys
	 * @param <V> The type of the map values
	 * @param m The map whose key must by sorted
	 * @return A list of key, sorted by their values in ascending order.
	 */
    public static <K, V> List<K> sortByValue(final Map<K, V> m) {
        List<K> keys = new ArrayList<K>();
        keys.addAll(m.keySet());
        Collections.sort(keys, new Comparator<K>() {
            public int compare(K o1, K o2) {
                V v1 = m.get(o1);
                V v2 = m.get(o2);
                if (v1 == null) {
                    return (v2 == null) ? 0 : 1;
                }
                else if (v1 instanceof Comparable) {
                    return ((Comparable<V>) v1).compareTo(v2);
                }
                else {
                    return 0;
                }
            }
        });
        return keys;
    }
    
    /**
     * Creates a new map, using the values of the initial one as keys for the new
     * one, and keys of the initial one as a set of values for the new one.
	 * @param <K> The type of the map keys
	 * @param <V> The type of the map values
     * @param m The initial map.
     * @return A new map built by inversing the keys and values of the initial one.
     * For each new key, a set is built for all the values (ie, the former keys).
     */
    public static <K, V> Map<V, Set<K>> reverseMap(final Map<K, V> m) {
    	Map<V, Set<K>> res = new HashMap<V, Set<K>>();
    	
    	for (K k : m.keySet()) {
    		if (!res.containsKey(m.get(k)))
    			res.put(m.get(k), new HashSet<K>());
    		res.get(m.get(k)).add(k);	
    	}
    	
    	
    	return res;
    }
    
    public static <K, V> Map<V, Set<K>> reverseSetMap(final Map<K, Set<V>> m) {
    	Map<V, Set<K>> res = new HashMap<V, Set<K>>();
    	
    	for (K k : m.keySet()) {
    		for (V v : m.get(k)) {
	    		if (!res.containsKey(v))
	    			res.put(v, new HashSet<K>());
	    		res.get(v).add(k);
    		}
    	}
    	
    	
    	return res;
    }
    
    /**
     * This simple method return true is a given object implements a given
     * interface.
     * It uses the Java reflection API.
     *  
     * @param o Any object
     * @param i An interface
     * @return true if the object implements the interface.
     */
    public static <T, I> boolean implementsInterface(T o, Class<I> i) {
    	for (Class<?> c : o.getClass().getInterfaces()) {
    		if (c.equals(i))
    			return true;
    	}
    	return false;
    }
    
    /**
     * Convert primitive and collection objects to a JSON-like string.
     * 
     * The conversion should be correct in standard cases, but it doesn't handle
     * corner cases.
     * @param o The object to convert
     * @return A JSON-like string representating the object.
     * @see <a href="http://json.org">The JSON website</a>
     */
    public static <T> String stringify(T o) {
    	if (o == null)
    		return "null";
    	else if (Helpers.implementsInterface(o, Set.class))
			return setToString((Set) o);
		else if (Helpers.implementsInterface(o, List.class))
			return listToString((List) o);
		else if (Helpers.implementsInterface(o, Map.class))
			return mapToString((Map) o);
		else if (o.getClass().getSuperclass().equals(Number.class) ||
				o.getClass().equals(Boolean.class) ||
				o.getClass().equals(Character.class))
			return o.toString();    			
		else return protectValue(o.toString());

	}
    
    private static <V> String listToString(List<V> list) {
		String str = "[";

		for (V v : list)
			str += stringify(v) + ",";
		
		str = (str.equals("[") ? str : str.substring(0, str.length() - 1)) + "]";
		
		return str;
	}


    private static <V> String setToString(Set<V> list) {
		String str = "[";
		
		for (V v : list)
			str += stringify(v) + ",";
		
		str = (str.equals("[") ? str : str.substring(0, str.length() - 1)) + "]";
		
		return str;
	}

    private static <K, V> String mapToString(Map<K, V> map) {
		String str = "{";
		for (Entry<K, V> es : map.entrySet()) {
			str += stringify(es.getKey()) + ":" + stringify(es.getValue()) + ",";
		}
		
		str = (str.equals("{") ? str : str.substring(0, str.length() - 1)) + "}";
		
		return str;
	}
    
    /** Deserializes a string received from a client to a Java native object.
     * 
     * @param <T> type of the expected deserialized object.
     * @param val the input string, in a JSON-like format
     * @param type type of the expected deserialized object.
     * @return the newly created object
     * @throws OntologyServerException thrown if an unparsable unicode character
     *  is encountered
     * @throws IllegalArgumentException thrown if the string couldn't be 
     * deserialized to the expect object. 
     */
    @SuppressWarnings("unchecked")
	public static <T> T deserialize(String val, Class<T> type) 
    				throws OntologyServerException, IllegalArgumentException {
    	
    		//Save it for errors message, in case something goes wrong
    		String originalValue = val;
		
			if (type == String.class)
				return (T) cleanValue(val);
					
			if (type == Integer.class)
				return (T) new Integer(Integer.parseInt(val));
			
			if (type == Double.class)
				return (T) new Double(Double.parseDouble(val));
			
			if (type == Boolean.class)
				return (T) new Boolean(Boolean.parseBoolean(val));
			
			//assumes it's a list or map
				
			boolean isValidMap = true;
			boolean isValidSet = true;
			
			if (val.isEmpty()) {
				if (Set.class.isAssignableFrom(type))
					return (T) new HashSet<String>();
				if (Map.class.isAssignableFrom(type))
					return (T) new HashMap<String, String>();				
			}
			
			//First, inspect the string to determine the type.
			//If it starts and ends with {}, it's a map
			//If it starts and ends with [], it's a set
			if ( !val.substring(0, 1).equals("{") || !val.substring(val.length() - 1, val.length()).equals("}"))
					isValidMap = false;
			
			if ( !val.substring(0, 1).equals("[") || !val.substring(val.length() - 1, val.length()).equals("]"))
					isValidSet = false;
			
		
			val = val.substring(1, val.length() - 1); //remove the [] or {}
			
			//checks that every elements of the map is made of tokens separated by :
			if (!val.equals("")) {
				for (String s : tokenize(val, ','))
					if (!isValidMap || !s.contains(":"))
						isValidMap = false;
			}
				
			
			
			assert(!(isValidMap && isValidSet));
			
			//if the string looks like a map and a map is indeed expected...
			if (isValidMap && Map.class.isAssignableFrom(type)){
				Map<String, String> result = new HashMap<String, String>();
				
				if (val.equals("")) return (T) result;
				
				for (String s : tokenize(val, ','))
					result.put(	cleanValue(s.trim().split(":", 2)[0]), 
							cleanValue(s.trim().split(":", 2)[1]));
				
				return (T) result;
			}					
			//if the string looks like a set and a set of a list is indeed expected...
			else if (isValidSet && Set.class.isAssignableFrom(type)){
				Set<String> result = new HashSet<String>();
				
				if (val.equals("")) return (T) result;
				
				for (String s : tokenize(val, ','))
					result.add(cleanValue(s));
				return (T) result;
			}					
			//if the string looks like a set and a list of a list is indeed expected...
			else if (isValidSet && List.class.isAssignableFrom(type)){
				List<String> result = new ArrayList<String>();
				
				if (val.equals("")) return (T) result;
				
				for (String s : tokenize(val, ','))
					result.add(cleanValue(s));
				return (T) result;
			}
			
			else throw new IllegalArgumentException("Unable to deserialize the" +
					" string! (a " + type.getSimpleName() + " was expected by" +
					" the method, received \"" + originalValue + "\" instead)");
	}
		  
	/** Remove leading and trailing quotes and whitespace if needed from a 
	 * string and unescape escaped sequences. 
	 * 
	 * @param value the string to clean.
	 * @return The same, unescaped string with quotes and whitespaces trimmed.
	 * @throws OntologyServerException if a unparsable unicode character is found
	 */
	public static String cleanValue(String value) throws OntologyServerException {
		String res = value.trim();
		if ((res.startsWith("\"") && res.endsWith("\"")) || (res.startsWith("'") && res.endsWith("'")))
			res = res.substring(1, res.length() - 1);
		
		res = unescapeJava(res);
		
		return res;
	}
	
	/** Protect a string by escaping the quotes and surrounding the string with quotes.
	 * 
	 * @param value The string to protect.
	 * @return The same string between double quotes and with inside quotes
	 * escaped. 
	 */
	public static String protectValue(String value) {
		String res = value.replaceAll("\"", "\\\"");
		    		
		return "\"" + res + "\"";
	}
	
	
	public static <T> T pickRandom(Set<T> set) {
		
		if(set.isEmpty()) return null;
		
		ArrayList<T> list = new ArrayList<T>(set);
		
		int size = set.size();
		
		if (size == 1)
			return list.get(0);

		return list.get(handOfGod.nextInt(size));		
	}
	
    /**
     * <p>Unescapes any Java literals found in the <code>String</code>.
     * For example, it will turn a sequence of <code>'\'</code> and
     * <code>'n'</code> into a newline character, unless the <code>'\'</code>
     * is preceded by another <code>'\'</code>.</p>
     *
     * This method comes from Apache Foundation, is released under Apache 
     * License 2.0 and can be found in org.apache.commons.lang.StringEscapeUtils
     *  
     * @param str  the <code>String</code> to unescape, may be null
     * @return a new unescaped <code>String</code>, <code>null</code> if null string input
     * @throws OntologyServerException if a unparsable unicode character is found
     */
    public static String unescapeJava(String str) throws OntologyServerException {
        if (str == null) {
            return null;
        }
        try {
            StringWriter writer = new StringWriter(str.length());
            unescapeJava(writer, str);
            return writer.toString();
        } catch (IOException ioe) {
            // this should never ever happen while writing to a StringWriter
            ioe.printStackTrace();
            return null;
        }
    }
    
    /**
     * <p>Unescapes any Java literals found in the <code>String</code> to a
     * <code>Writer</code>.</p>
     *
     * <p>For example, it will turn a sequence of <code>'\'</code> and
     * <code>'n'</code> into a newline character, unless the <code>'\'</code>
     * is preceded by another <code>'\'</code>.</p>
     *
     * <p>A <code>null</code> string input has no effect.</p>
     *
     * This method comes from Apache Foundation, is released under Apache 
     * License 2.0 and can be found in org.apache.commons.lang.StringEscapeUtils 
     * @param out  the <code>Writer</code> used to output unescaped characters
     * @param str  the <code>String</code> to unescape, may be null
     * @throws IllegalArgumentException if the Writer is <code>null</code>
     * @throws IOException if error occurs on underlying Writer
     * @throws OntologyServerException if a unparsable unicode character is found
     */
    public static void unescapeJava(Writer out, String str) throws IOException, OntologyServerException {
        if (out == null) {
            throw new IllegalArgumentException("The Writer must not be null");
        }
        if (str == null) {
            return;
        }
        int sz = str.length();
        StringBuffer unicode = new StringBuffer(4);
        boolean hadSlash = false;
        boolean inUnicode = false;
        for (int i = 0; i < sz; i++) {
            char ch = str.charAt(i);
            if (inUnicode) {
                // if in unicode, then we're reading unicode
                // values in somehow
                unicode.append(ch);
                if (unicode.length() == 4) {
                    // unicode now contains the four hex digits
                    // which represents our unicode character
                    try {
                        int value = Integer.parseInt(unicode.toString(), 16);
                        out.write((char) value);
                        unicode.setLength(0);
                        inUnicode = false;
                        hadSlash = false;
                    } catch (NumberFormatException nfe) {
                        throw new OntologyServerException("Unable to parse unicode value: " + unicode);
                    }
                }
                continue;
            }
            if (hadSlash) {
                // handle an escaped value
                hadSlash = false;
                switch (ch) {
                    case '\\':
                        out.write('\\');
                        break;
                    case '\'':
                        out.write('\'');
                        break;
                    case '\"':
                        out.write('"');
                        break;
                    case 'r':
                        out.write('\r');
                        break;
                    case 'f':
                        out.write('\f');
                        break;
                    case 't':
                        out.write('\t');
                        break;
                    case 'n':
                        out.write('\n');
                        break;
                    case 'b':
                        out.write('\b');
                        break;
                    case 'u':
                        {
                            // uh-oh, we're in unicode country....
                            inUnicode = true;
                            break;
                        }
                    default :
                        out.write(ch);
                        break;
                }
                continue;
            } else if (ch == '\\') {
                hadSlash = true;
                continue;
            }
            out.write(ch);
        }
        if (hadSlash) {
            // then we're in the weird case of a \ at the end of the
            // string, let's output it anyway.
            out.write('\\');
        }
    }



}
