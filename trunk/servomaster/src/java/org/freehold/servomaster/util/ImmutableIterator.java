package org.freehold.servomaster.util;

import java.util.Iterator;

/**
 * An immutable wrapper for {@link java.util.Iterator java.util.Iterator}.
 *
 * Sloppy developers often expose privileged objects through an iterator. 
 * This class makes sure that {@link #remove remove()} method will not alter
 * the state of the collection being iterated.
 *
 * @author <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a>
 * @version $Id: ImmutableIterator.java,v 1.3 2005-02-03 07:05:56 vtt Exp $
 */
public class ImmutableIterator implements Iterator {

    /**
     * An iterator to protect.
     */
    private Iterator target;
    
    /**
     * Create an instance.
     *
     * @param target Iterator to protect.
     */
    public ImmutableIterator(Iterator target) {
    
        if ( target == null ) {
        
            throw new IllegalArgumentException("Target can't be null");
        }
        
        this.target = target;
    }
    
    /**
     * Returns <code>true</code> if the iteration has more elements. (In
     * other words, returns <code>true</code> if <code>next()</code> would
     * return an element rather than throwing an exception.)
     *
     * @return <code>true</code> if the iterator has more elements.
     */
    public boolean hasNext() {
    
        return target.hasNext();
    }
    
    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     *
     * @exception NoSuchElementException if iteration has no more elements.
     */
    public Object next() {
    
        return target.next();
    }
    
    /**
     * Throws <code>UnsupportedOperationException</code> exception.
     *
     * @exception UnsupportedOperationException this class doesn't support
     * <code>remove()</code>.
     */
    public void remove() {
    
        throw new UnsupportedOperationException("Can't remove an item from under immutable iterator");
    }
}
