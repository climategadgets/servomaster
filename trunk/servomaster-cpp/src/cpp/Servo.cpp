// $Id: Servo.cpp,v 1.7 2003-09-03 05:35:32 vtt Exp $
#include <Servo.h>
#include <stddef.h>
#include <stdio.h>
#include <stdexcept>

namespace servomaster {

    Servo::Servo(ServoController *servoController, Servo *target) :
        position(0),
        enabled(true),
        servoController(servoController),
        target(target),
        transitionController(NULL),
        transitionDriver(NULL),
        actualPosition(0) {
    
    }
    
    Servo::~Servo() {
    
    }
    
    double Servo::getPosition() {
    
        return position;
    }
    
    double Servo::getActualPosition() {
    
        return actualPosition;
    }
    
    ServoController *Servo::getController() {
    
        return servoController;
    }
    
    TransitionController *Servo::attach(TransitionController *transitionController) {
    
        // VT: FIXME: Get the synchronization lock
        
        Servo *s = getTarget();
        
        while ( s != NULL ) {
        
            if ( s->getTransitionController() != NULL ) {
            
                throw std::runtime_error("Can't attach more than one transition controller in a stack");
            }
            
            s = s->getTarget();
        }
        
        TransitionController *old = this->transitionController;
        
        this->transitionController = transitionController;
        
        return old;
    }
    
    TransitionController *Servo::getTransitionController() {
    
        return transitionController;
    }
    
    Servo *Servo::getTarget() {
    
        return target;
    }
    
    void Servo::setPosition(double position) {
    
        // VT: FIXME: Check if it is enabled and if not, throw an exception
        
        // VT: FIXME: Get the synchronization lock
        
        {
        
            this->position = position;
            
            if ( transitionController != NULL ) {
            
                if ( transitionDriver != NULL ) {
                
                    transitionDriver->stop();
                }
                
                transitionDriver = new servo::TransitionDriver(this, position);
                transitionDriver->start();
            
            } else {
            
                printf("setActualPosition(%3.3F)\n", position);
            
                setActualPosition(position);
            }
        }
    }
    
    void Servo::checkPosition(double position) {
    
        if ( position < 0 || position > 1.0 ) {
        
            throw std::runtime_error("Requested position outside of 0...1 range");
        }
    }
    
    namespace servo {
    
        TransitionDriver::TransitionDriver(Servo *target, double position) : target(target), position(position) {
        }
        
        void TransitionDriver::start() {
        }
        
        void TransitionDriver::stop() {
        }
    }
}
