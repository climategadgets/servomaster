#include <PhidgetServoController.h>
#include <usb.h>
#include <stdio.h>

namespace servomaster {

    phidget::ControllerDescriptor *PhidgetServoController::modelTable[] = {
    
        new phidget::ControllerDescriptor("QuadServo", 0x06C2, 0x0038),
        new phidget::ControllerDescriptor("AdvancedServo", 0x06C2, 0x003B),
        NULL
    };

    void PhidgetServoController::init(const char *portName) {
    
        struct usb_bus *bus;
        struct usb_device *dev;
        
        usb_init();
        usb_find_busses();
        usb_find_devices();
        
        for ( bus = usb_busses; bus != NULL; bus = bus->next ) {
        
            for ( dev = bus->devices; dev != NULL; dev = dev->next ) {
            
                printf("Found %x:%x\n", dev->descriptor.idVendor, dev->descriptor.idProduct);

                for ( int idx = 0; modelTable[idx] != NULL; idx++ ) {
                
                    if (    modelTable[idx]->vendorID == dev->descriptor.idVendor
                         && modelTable[idx]->productID == dev->descriptor.idProduct ) {
                         
                        
                        printf("Found %s\n", modelTable[idx]->model);
                    }
                }
            }
        }
    }
    
    void PhidgetServoController::checkInit() {
    
        Object::checkInit();
    }
    
    bool PhidgetServoController::isConnected() {
    
        // VT: FIXME
        
        return true;
    }
    
    namespace phidget {
    
        ControllerDescriptor::ControllerDescriptor(const char *model, int vendorID, int productID) :
            model(model),
            vendorID(vendorID),
            productID(productID) {
        }
    }
}
