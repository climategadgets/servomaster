package net.sf.servomaster.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An immutable wrapper for {@link Iterator java.util.Iterator}.
 *
 * Sloppy developers often expose privileged objects through an iterator.
 * This class makes sure that {@link #remove remove()} method will not alter
 * the state of the collection being iterated.
 *
 * <p>
 *
 * VT: FIXME: Since this class was created, {@link Collections} with their immutable operations
 * arrived. May need to retire this class - there's no point to have it anymore.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class ImmutableIterator<T> implements Iterator<T> {

    /**
     * An iterator to protect.
     */
    private Iterator<T> target;

    /**
     * Create an instance.
     *
     * @param target Iterator to protect.
     */
    public ImmutableIterator(Iterator<T> target) {

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
    public T next() {

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
