// $Id: ServoController.h,v 1.5 2003-09-03 06:20:01 vtt Exp $
#ifndef SERVOCONTROLLER_H
#define SERVOCONTROLLER_H

// Implies Object.h
#include <Servo.h>

namespace servomaster {

    class Servo;
    class ServoController : protected Object {
    
        protected:
        
            bool connected;
            bool disconnected;
            char *portName;
            virtual void checkInit() = 0;
            
        public:
        
            ServoController();
        
            const char *getPort();
            virtual void init(const char *portName) = 0;
//            bool isLazy();
//            void reset();
//            void setLazyMode(bool lazy);
            virtual bool isConnected() = 0;
            
            virtual Servo *getServo(const char *id) = 0;
    };
}

#endif
