#include <PhidgetServoController.h>
#include <stddef.h>

int main() {

    servomaster::PhidgetServoController c;
    
    c.init(NULL);
    
    return 0;
}
