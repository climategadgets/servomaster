#ifndef PHIDGETSERVOCONTROLLER_H
#define PHIDGETSERVOCONTROLLER_H

#include <ServoController.h>

namespace servomaster {

    namespace phidget {
    
        class ControllerDescriptor {
        
            public:
            
                const char *model;
                int vendorID;
                int productID;
                
                ControllerDescriptor(const char *model, int vendorID, int productID);
        };
    
    }

    class PhidgetServoController : protected ServoController {
    
        private:
        
            static phidget::ControllerDescriptor *modelTable[];
            
        protected:

            virtual void checkInit();

        public:
        
            virtual void init(const char *portName);
            virtual bool isConnected();
    };
}

#endif
