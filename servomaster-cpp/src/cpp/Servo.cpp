// $Id: Servo.cpp,v 1.8 2003-09-03 06:06:48 vtt Exp $
#include <Servo.h>
#include <stddef.h>
#include <stdio.h>
#include <stdexcept>

namespace servomaster {

    Servo::Servo(ServoController *_servoController, Servo *_target) :
        position(0),
        enabled(true),
        servoController(_servoController),
        target(_target),
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
    
    TransitionController *Servo::attach(TransitionController *_transitionController) {
    
        // VT: FIXME: Get the synchronization lock
        
        Servo *s = getTarget();
        
        while ( s != NULL ) {
        
            if ( s->getTransitionController() != NULL ) {
            
                throw std::runtime_error("Can't attach more than one transition controller in a stack");
            }
            
            s = s->getTarget();
        }
        
        TransitionController *old = this->transitionController;
        
        this->transitionController = _transitionController;
        
        return old;
    }
    
    TransitionController *Servo::getTransitionController() {
    
        return transitionController;
    }
    
    Servo *Servo::getTarget() {
    
        return target;
    }
    
    void Servo::setPosition(double _position) {
    
        // VT: FIXME: Check if it is enabled and if not, throw an exception
        
        // VT: FIXME: Get the synchronization lock
        
        {
        
            this->position = _position;
            
            if ( transitionController != NULL ) {
            
                if ( transitionDriver != NULL ) {
                
                    transitionDriver->stop();
                }
                
                transitionDriver = new servo::TransitionDriver(this, _position);
                transitionDriver->start();
            
            } else {
            
                printf("setActualPosition(%3.3F)\n", _position);
            
                setActualPosition(_position);
            }
        }
    }
    
    void Servo::checkPosition(double _position) {
    
        if ( _position < 0 || _position > 1.0 ) {
        
            throw std::runtime_error("Requested position outside of 0...1 range");
        }
    }
    
    namespace servo {
    
        TransitionDriver::TransitionDriver(Servo *_target, double _position) :
            target(_target),
            position(_position) {
        }
        
        void TransitionDriver::start() {
        }
        
        void TransitionDriver::stop() {
        }
    }
}
