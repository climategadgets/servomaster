#include <PhidgetServoController.h>
#include <stdio.h>
#include <string.h>
#include <stdexcept>

namespace servomaster {

    phidget::ControllerDescriptor *PhidgetServoController::modelTable[] = {
    
        new phidget::ControllerDescriptor("QuadServo", 0x06C2, 0x0038),
        new phidget::ControllerDescriptor("AdvancedServo", 0x06C2, 0x003B),
        NULL
    };

    void PhidgetServoController::init(const char *portName) {
    
        printf("Init: port %s\n", portName);
    
        struct usb_bus *bus;
        struct usb_device *dev;
        
        // USB has a maximum of 256 devices, so this should be more than
        // enough
        
        phidget::UsbContext *found[256];
        int totalFound = 0;
        
        usb_init();
        usb_find_busses();
        usb_find_devices();
        
        for ( bus = usb_busses; bus != NULL; bus = bus->next ) {
        
            for ( dev = bus->devices; dev != NULL; dev = dev->next ) {
            
                printf("Found %x:%x\n", dev->descriptor.idVendor, dev->descriptor.idProduct);

                for ( int idx = 0; modelTable[idx] != NULL; idx++ ) {
                
                    if (    modelTable[idx]->vendorID == dev->descriptor.idVendor
                         && modelTable[idx]->productID == dev->descriptor.idProduct ) {
                         
                        found[totalFound] = new phidget::UsbContext(dev, modelTable[idx]);

                        printf("Found %s serial #%s\n", modelTable[idx]->model, found[totalFound]->getSerial());
                        
                        totalFound++;
                    }
                }
            }
        }
        
        // Check how many servo controllers we've found.
        
        int rightIndex = -1;
        
        if ( portName == NULL ) {
        
            // If the portName parameter is null, one servo controller is
            // expected to be found. If there's none or more than one, boom.
            
            if ( totalFound != 1 ) {
            
                throw runtime_error("None or more than one servo controller was found, but port name was not specified");
            }
            
            rightIndex = 0;

        } else {
        
            // If the portName parameter is not null, this meanst that the
            // caller expected to find a specific controller. In this case:
            //
            // - Found none: boom
            // - Found one or more, and the serial number on one of them is
            //   equal to portName: return it
            // - Found one or more, no serial number matches with the
            //   portName: boom
            
            if ( totalFound == 0 ) {
            
                throw runtime_error("No servo controllers found");
            }
            
            for ( int idx = 0; idx < totalFound; idx++ ) {
            
                const char *serial = found[idx]->getSerial();
                
                if ( serial == NULL ) {
                
                    continue;
                }
            
                if ( strcmp(serial, portName) == 0 ) {
                
                    // We've found the one they were looking for
                    
                    rightIndex = idx;
                    break;
                }
            }
            
            if ( rightIndex == -1 ) {
            
                // VT: FIXME: Handle the disconnected case as well
                
                throw runtime_error("Servo controller with requested serial is not present");
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
        
        UsbContext::UsbContext(struct usb_device *device, ControllerDescriptor *cd) :
            ControllerDescriptor(*cd),
            serial(NULL) {
        
            this->device = device;
        }
        
        const char *UsbContext::getSerial() {
        
            // VT: FIXME: Get the synchronization lock
            
            // VT: NOTE: Until the lock is not provided, it's in our best
            // interests to minimize the risk. Therefore, we better do it
            // fast...
        
            if ( serial != NULL ) {
            
                return (const char *)serial;
            }
            
            // Damn, we have to retrieve it now...
            
            char buffer[16];
            
            // VT: FIXME: Handle the case when the device is not available
            // anymore
            
            handle = usb_open(device);
            
            if ( handle == NULL ) {
            
                throw runtime_error("Can't open USB device");
            }
            
            int rc = usb_control_msg(handle, 0x80, 0x06, 0x0303, 0, buffer, sizeof(buffer), 5000);
            
            if ( rc == -1 ) {
            
                throw runtime_error("USB read failure");
            }
            
            serial = (char *)malloc(sizeof(buffer));
            
	    // The result is a Unicode string, have to parse it
	    
	    // VT: FIXME: Why the hell do I have to circumcise the Unicode
	    // string? I'd better store it as Unicode, when I figure out
	    // how, that is...
	    
	    int current = 0;
	    
	    if (buffer[0] == 12 && buffer[1] == 3) {
	    
		for ( int i = 0; i <= 10; i++ ) {
		
		    if ( buffer[i + 2] >= 48 && buffer[i + 2] <= 57 ) {
		    
			serial[current] = buffer[i + 2];
			current++;
		    }
		}
	    }
	    
	    serial[current] = 0;
	    
            return (const char *)serial;
        }
    }
}
