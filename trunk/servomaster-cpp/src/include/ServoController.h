// $Id: ServoController.h,v 1.4 2002-09-13 08:37:53 vtt Exp $
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
            const char *portName;
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
