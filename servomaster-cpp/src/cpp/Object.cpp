#include <Object.h>
#include <stdexcept>

namespace servomaster {

    Object::Object() : initialized(false) {
    }
    
    void Object::checkInit() {
    
        if ( !initialized ) {
        
            throw runtime_error("Not initialized");
        }
    }
}
