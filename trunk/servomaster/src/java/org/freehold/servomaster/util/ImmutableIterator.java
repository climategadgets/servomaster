package org.freehold.servomaster.util;

import java.util.Iterator;

public class ImmutableIterator implements Iterator {

    private Iterator target;
    
    public ImmutableIterator(Iterator target) {
    
        if ( target == null ) {
        
            throw new IllegalArgumentException("Target can't be null");
        }
        
        this.target = target;
    }
    
    public boolean hasNext() {
    
        return target.hasNext();
    }
    
    public Object next() {
    
        return target.next();
    }
    
    public void remove() {
    
        throw new IllegalAccessError("Can't remove an item from under immutable iterator");
    }
}
