// $Id: TransitionController.h,v 1.2 2002-09-13 08:37:53 vtt Exp $
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
