#include <PhidgetServoController.h>
#include <stddef.h>
#include <stdio.h>
#include <stdexcept>

int main() {

    try {
    
        servomaster::PhidgetServoController c;
        
        c.init(NULL);
        
    } catch ( const exception &e ) {
    
        printf("Problem: %s\n", e.what());
        return 1;
    }
    
    return 0;
}
