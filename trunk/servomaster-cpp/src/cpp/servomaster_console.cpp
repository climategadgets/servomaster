// $Id: servomaster_console.cpp,v 1.6 2003-09-03 06:44:23 vtt Exp $
#include <PhidgetServoController.h>
#include <stddef.h>
#include <stdio.h>
#include <unistd.h>
#include <stdexcept>

int main() {

    try {
    
        servomaster::PhidgetServoController c;
        
        c.init(NULL);
        
        servomaster::Servo *s = c.getServo("0");
        
        s->setPosition(1.0);
        
        printf("Servo set to 1.0\n");
        
        sleep(1);
        
        s->setPosition(0);
        
        printf("Servo set to 0.0\n");
        
    } catch ( const std::exception &e ) {
    
        printf("Problem: %s\n", e.what());
        return 1;
    }
    
    printf("Finished\n");
    
    return 0;
}
