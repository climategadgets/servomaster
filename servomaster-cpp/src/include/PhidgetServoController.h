#ifndef PHIDGETSERVOCONTROLLER_H
#define PHIDGETSERVOCONTROLLER_H

#include <ServoController.h>
#include <usb.h>

namespace servomaster {

    namespace phidget {
    
        class ControllerDescriptor {
        
            public:
            
                const char *model;
                int vendorID;
                int productID;
                
                ControllerDescriptor(const char *model, int vendorID, int productID);
        };
        
        class UsbContext : public ControllerDescriptor {
        
            public:
            
                struct usb_dev_handle *handle;
                struct usb_device *device;
                char *serial;
                
                UsbContext(struct usb_device *device, ControllerDescriptor *cd);
                const char *getSerial();
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
