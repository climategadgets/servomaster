#include <Object.h>
#include <stdio.h>
#include <stdexcept>

namespace servomaster {

    long Object::instanceCount = 0;

    Object::Object() : initialized(false) {
    
        instanceCount++;
    
        printf("Object: %d created %X\n", instanceCount, this);
    }
    
    Object::~Object() {
    
        instanceCount--;
        
        printf("Object: destroyed %X, %d left\n", this, instanceCount);
    }
    
    void Object::checkInit() {
    
        if ( !initialized ) {
        
            throw runtime_error("Not initialized");
        }
    }
}
