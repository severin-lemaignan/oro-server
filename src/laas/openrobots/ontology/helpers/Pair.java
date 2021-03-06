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

import java.util.ArrayList;
import java.util.List;

public class Pair<L, R> {
 
    private final L left;
    private final R right;
 
    public R getRight() {
        return right;
    }
 
    public L getLeft() {
        return left;
    }
 
    public Pair(final L left, final R right) {
        this.left = left;
        this.right = right;
    }
    
    public static <A, B> Pair<A, B> create(A left, B right) {
        return new Pair<A, B>(left, right);
    }
 
    @Override
	public final boolean equals(Object o) {
        if (!(o instanceof Pair))
            return false;
 
        final Pair<?, ?> other = (Pair) o;
        return equal(getLeft(), other.getLeft()) && equal(getRight(), other.getRight());
    }
    
    public static final boolean equal(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }
 
    @Override
	public int hashCode() {
        int hLeft = getLeft() == null ? 0 : getLeft().hashCode();
        int hRight = getRight() == null ? 0 : getRight().hashCode();
 
        return hLeft + (57 * hRight);
    }
    
    @Override
	public String toString() {
    	return "<" + getLeft().toString() + ", " + getRight().toString() + ">";
    }
    
    /**
     * If the left and right members of the pair have the same type, return a list of the members.
     * Else, return null
     * 
     * TODO Test this code!!
     */
    public List<L> asList() {
    	
    	//if (!left.getClass().equals(right.getClass()))
    	//	return null;
    	
    	List<L> res =  new ArrayList<L>();
    	res.add(left);

		try {
			res.add((L) right);
		} catch (Exception e) {
			e.printStackTrace();
		}
    		
    	return res;
    }
}
