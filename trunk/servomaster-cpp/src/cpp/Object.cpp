// $Id: Object.cpp,v 1.5 2003-09-03 05:35:32 vtt Exp $
#include <Object.h>
#include <stdio.h>
#include <stdexcept>

namespace servomaster {

    long Object::instanceCount = 0;

    Object::Object() : initialized(false) {
    
        instanceCount++;
    
        printf("Object: %ld created %X\n", instanceCount, (unsigned int)this);
    }
    
    Object::~Object() {
    
        instanceCount--;
        
        printf("Object: destroyed %X, %ld left\n", (unsigned int)this, instanceCount);
    }
    
    void Object::checkInit() {
    
        if ( !initialized ) {
        
            throw std::runtime_error("Not initialized");
        }
    }
}
