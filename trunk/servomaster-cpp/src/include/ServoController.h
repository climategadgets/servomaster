#ifndef SERVOCONTROLLER_H
#define SERVOCONTROLLER_H

#include <Object.h>

namespace servomaster {

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
            bool isLazy();
            void reset();
            void setLazyMode(bool lazy);
            virtual bool isConnected() = 0;
    };
}

#endif
