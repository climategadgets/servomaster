// $Id: Object.cpp,v 1.6 2003-09-03 07:08:37 vtt Exp $
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
        
        printf("Object: destroyed %8X, %ld left\n", (unsigned int)this, instanceCount);
    }
    
    void Object::checkInit() {
    
        if ( !initialized ) {
        
            throw std::runtime_error("Not initialized");
        }
    }
}
