// $Id: servomaster_console.cpp,v 1.7 2003-09-03 08:07:08 vtt Exp $
#include <PhidgetServoController.h>
#include <iostream.h>
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
    
        //printf("Problem: %s\n", e.what());
        
        cerr << "Exception: " << e.what() << endl;
        return 1;
    }
    
    printf("Finished\n");
    
    return 0;
}
