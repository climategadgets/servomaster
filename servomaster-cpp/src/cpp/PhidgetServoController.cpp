// $Id: PhidgetServoController.cpp,v 1.7 2002-09-14 03:32:56 vtt Exp $
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

    PhidgetServoController::PhidgetServoController() : thePhidgetServo(NULL) {
    }
    
    PhidgetServoController::~PhidgetServoController() {
    
        // VT: FIXME: Set the servos to predefined positions and shut off
        // the pulse
    
        // VT: FIXME: Close it first
        
        delete thePhidgetServo;
    
        for ( int idx = 0; idx < protocolHandler->getServoCount(); idx++ ) {
        
            Servo *s = servoSet[idx];
            
            if ( s != NULL ) {
            
                delete s;
            }
        }
        
        free(servoSet);
        free(servoPosition);
        
        // VT: FIXME: shut it down first
        delete protocolHandler;
    }
    
    void PhidgetServoController::init(const char *portName) {
    
        if ( this->portName != NULL ) {
        
            printf("Port name: %s\n", this->portName);
            throw runtime_error("Already initialized");
        }
        
        printf("Init: port %s\n", portName);
    
        thePhidgetServo = findUSB(portName);
        
        // VT: FIXME: Handle the disconnected case

        this->portName = (const char *)malloc(sizeof(char) *strlen(thePhidgetServo->getSerial()));
        
        strcpy((char *)this->portName, thePhidgetServo->getSerial());
        
        switch ( thePhidgetServo->getProtocolHandlerId() ) {
        
            case 0x06C20038:
            
                protocolHandler = new phidget::ProtocolHandler003;
                break;
                
            case 0x06C2003B:
            
                protocolHandler = new phidget::ProtocolHandler004;
                break;
                
            default:
            
                printf("Vendor/product ID: %8X\n", thePhidgetServo->getProtocolHandlerId());
                throw runtime_error("Unknown vendor/product ID combination");
        }
        
        int count = protocolHandler->getServoCount();
        servoSet = (Servo **)malloc(sizeof(Servo *) * count);
        servoPosition = (int *)malloc(sizeof(int) * count);
        
        for ( int idx = 0; idx < count; idx++ ) {
        
            servoSet[idx] = NULL;
            servoPosition[idx] = 0;
        }
    }
    
    phidget::UsbContext *PhidgetServoController::findUSB(const char *portName) {
    
        phidget::UsbContext **found = findUSB();
        
        // Check how many servo controllers we've found.
        
        int totalFound = 0;
        
        while ( found[totalFound] != NULL ) {
        
            totalFound++;
        }
        
        phidget::UsbContext *theRightOne = NULL;
        
        if ( portName == NULL ) {
        
            // If the portName parameter is null, one servo controller is
            // expected to be found. If there's none or more than one, boom.
            
            if ( totalFound != 1 ) {
            
                throw runtime_error("None or more than one servo controller was found, but port name was not specified");
            }
            
            // We have to free the buffer
            
            phidget::UsbContext *result = found[0];
            
            free(found);
            
            return result;

        }
        
        // If the portName parameter is not null, this meanst that the
        // caller expected to find a specific controller. In this case:
        //
        // - Found none: boom
        // - Found one or more, and the serial number on one of them is
        //   equal to portName: return it
        // - Found one or more, no serial number matches with the
        //   portName: boom
        
        // VT: FIXME: Handle the disconnected case as well
        
        if ( totalFound == 0 ) {
        
            throw runtime_error("No servo controllers found");
        }
        
        for ( int idx = 0; idx < totalFound; idx++ ) {
        
            const char *serial = found[idx]->getSerial();
            
            if ( serial == NULL ) {
            
                // VT: FIXME: How can it be???
                
                continue;
            }
        
            if ( strcmp(serial, portName) == 0 ) {
            
                // We've found the one they were looking for
                
                theRightOne = found[idx];
                
                // Let's proceed, so we can delete the rest
                
                continue;
            }
            
            free(found[idx]);
        }
        
        free(found);
        
        if ( theRightOne == NULL ) {
        
            throw runtime_error("Servo controller with requested serial is not present");
        }
        
        return theRightOne;
    }
    
    phidget::UsbContext **PhidgetServoController::findUSB() {
    
        struct usb_bus *bus;
        struct usb_device *dev;
        
        // USB has a maximum of 128 devices, so this should be more than
        // enough
        
        phidget::UsbContext **found = (phidget::UsbContext **)malloc(sizeof(phidget::UsbContext *) * 128);
        int totalFound = 0;
        
        // VT: FIXME: Figure out if usb_init() is idempotent. It better
        // be...
        
        usb_init();
        usb_find_busses();
        usb_find_devices();
        
        for ( bus = usb_busses; bus != NULL; bus = bus->next ) {
        
            for ( dev = bus->devices; dev != NULL; dev = dev->next ) {
            
                printf("Found %x:%x\n", dev->descriptor.idVendor, dev->descriptor.idProduct);

                for ( int idx = 0; modelTable[idx] != NULL; idx++ ) {
                
                    if (    modelTable[idx]->vendorID == dev->descriptor.idVendor
                         && modelTable[idx]->productID == dev->descriptor.idProduct ) {
                         
                        phidget::UsbContext *pFound = new phidget::UsbContext(dev, modelTable[idx]);

                        printf("Found %s serial #%s\n", modelTable[idx]->model, pFound->getSerial());
                        
                        found[totalFound++] = pFound;
                    }
                }
            }
        }
        
        found[totalFound] = NULL;
        
        // VT: FIXME: Reallocate the array to proper size
    
        return found;
    }
    
    void PhidgetServoController::checkInit() {
    
        Object::checkInit();
    }
    
    bool PhidgetServoController::isConnected() {
    
        // VT: FIXME
        
        return true;
    }
    
    Servo *PhidgetServoController::createServo(int id) {
    
        return new phidget::PhidgetServo(this, id);
    }
    
    Servo *PhidgetServoController::getServo(const char *id) {
    
        // VT: FIXME: Get the synchronization lock
        
        int idx = atoi(id);
        
        printf("getServo: %d\n", idx);
        
        if ( servoSet[idx] == NULL ) {
        
            printf("Need to create servo\n");
            servoSet[idx] = createServo(idx);
        }
        
        return servoSet[idx];
    }
    
    void PhidgetServoController::send() {
    
        printf("send\n");
    
        // VT: FIXME: Get the synchronization lock
    
        if ( thePhidgetServo == NULL ) {
        
            thePhidgetServo = findUSB(portName);
            connected = true;
        }
        
        unsigned char *buffer = protocolHandler->composeBuffer(servoPosition);
        send(buffer, protocolHandler->getBufferSize());
    }
    
    void PhidgetServoController::send(unsigned char *buffer, int size) {
    
        printf("send buffer\n");
    
        int rc = usb_control_msg(thePhidgetServo->handle, 0x21, 0x09, 0x200, 0, (char *)buffer, size, 5000);
        
        if ( rc != 0 ) {
        
            printf("usb_control_msg: rc=%x\n", rc);
        }
    }
    
    namespace phidget {
    
        ControllerDescriptor::ControllerDescriptor(const char *model, int vendorID, int productID) :
            model(model),
            vendorID(vendorID),
            productID(productID) {
        }
        
        unsigned long ControllerDescriptor::getProtocolHandlerId() const {
        
            return (vendorID << 16) | productID;
        }
                
        UsbContext::UsbContext(struct usb_device *device, ControllerDescriptor *cd) :
            ControllerDescriptor(*cd),
            serial(NULL) {
        
            this->device = device;
            printf("UsbContext: created: %s %x\n", model, this);
        }
        
        UsbContext::~UsbContext() {
        
            printf("UsbContext: destroyed: %s %x #%s\n", model, this, serial);
            
            free(serial);
        }
        
        const char *UsbContext::getSerial() {
        
            int rc;
            
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
            
            rc = usb_set_configuration(handle, 1);

            if ( rc == -1 ) {
            
                throw runtime_error("usb_set_configuration");
            }

            rc = usb_claim_interface(handle, 0);

            if ( rc == -1 ) {
            
                throw runtime_error("usb_claim_interface");
            }
            
            rc = usb_set_altinterface(handle, 0);

            if ( rc == -1 ) {
            
                throw runtime_error("usb_set_altinterface");
            }
            
            rc = usb_control_msg(handle, 0x80, 0x06, 0x0303, 0, buffer, sizeof(buffer), 5000);
            
            if ( rc == -1 ) {
            
                throw runtime_error("usb_control_msg");
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
        
        int ProtocolHandler003::getServoCount() {
        
            return 4;
        }
        
        int ProtocolHandler003::getBufferSize() {
        
            return 6;
        }
        
        unsigned char *ProtocolHandler003::composeBuffer(int servoPosition[]) {
        
            // We assume that the positions array contains 4 positions
            
            buffer[0]  = (unsigned char)(servoPosition[0] % 256);
            buffer[1]  = (unsigned char)(servoPosition[0] / 256);
            
            buffer[2]  = (unsigned char)(servoPosition[1] % 256);
            buffer[1] |= (unsigned char)((servoPosition[1] / 256) * 16);
            
            buffer[3]  = (unsigned char)(servoPosition[2] % 256);
            buffer[4]  = (unsigned char)(servoPosition[2] / 256);
            
            buffer[5]  = (unsigned char)(servoPosition[3] % 256);
            buffer[4] |= (unsigned char)((servoPosition[3] / 256) * 16);
            
        
            return (unsigned char *)&buffer;
        }

        int ProtocolHandler004::getServoCount() {
        
            return 8;
        }
        
        int ProtocolHandler004::getBufferSize() {
        
            throw runtime_error("ProtocolHandler004::getBufferSize(): Not Implemented");
        }
        
        unsigned char *ProtocolHandler004::composeBuffer(int servoPosition[]) {
        
            // We assume that the positions array contains 8 positions
            
            return NULL;
        }
        
        PhidgetServo::PhidgetServo(ServoController *controller, int id) :
            Servo(controller, NULL),
            id(id),
            min_pulse(1000),
            max_pulse(2000) {
            
            printf("PhidgetServo: created #%X: %X\n", id, this);
        }
        
        PhidgetServo::~PhidgetServo() {
        
            // There's nothing to do with the servo when this object is
            // being destroyed. This object's lifetime is defined by the
            // controller lifetime, and the controller will destroy the
            // servo right before destroying itself, so we don't do anything
            // here.
        
            printf("PhidgetServo: destroyed #%X: %X\n", id, this);
        }
        
        void PhidgetServo::setActualPosition(double position) {
        
            //checkInit();
            checkPosition(position);
            
            // Tough stuff, we're dealing with timing now...
            
            int microseconds = (int)(min_pulse + (position * (max_pulse - min_pulse)));
            
            // VT: NOTE: We need to know all the servo's positions because
            // they get transmitted in one packet
            
            PhidgetServoController *controller = (PhidgetServoController *)getController();
            
            controller->servoPosition[id] = microseconds;
            
            if ( true ) {
            
                printf("Position:     %f\n", position);
                printf("Microseconds: %d\n", microseconds);
                printf("Buffer:       %d\n", controller->servoPosition[id]);
            }
            
            controller->send();
            
            this->actualPosition = position;
            //actualPositionChanged();
        }
    }
}
