#ifndef TRANSITIONCONTROLLER_H
#define TRANSITIONCONTROLLER_H

#include <Object.h>
#include <Servo.h>
#include <TransitionToken.h>

namespace servomaster {

    class Servo;
    class TransitionController : Object {
    
        public:
        
            virtual TransitionToken &move(Servo &target, TransitionToken &token, double targetPosition);
    };
}

#endif
