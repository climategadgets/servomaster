#ifndef PHIDGETSERVOCONTROLLER_H
#define PHIDGETSERVOCONTROLLER_H

#include <ServoController.h>
#include <Servo.h>
#include <usb.h>

namespace servomaster {

    class PhidgetServoController;

    namespace phidget {
    
        /**
         * Servo controller type identifier.
         */
        class ControllerDescriptor {
        
            public:
            
                /**
                 * Device model name.
                 */
                const char *model;
                
                /**
                 * Device USB vendor ID.
                 */
                unsigned int vendorID;
                
                /**
                 * Device USB product ID.
                 */
                unsigned int productID;
                
                ControllerDescriptor(const char *model, int vendorID, int productID);
                unsigned long getProtocolHandlerId() const;
        };
        
        /**
         * USB context of the phidget.
         */
        class UsbContext : public ControllerDescriptor {
        
            public:
            
                struct usb_dev_handle *handle;
                struct usb_device *device;
                char *serial;
                
                UsbContext(struct usb_device *device, ControllerDescriptor *cd);
                ~UsbContext();
                const char *getSerial();
        };
        
        class PhidgetServo : public Servo {
        
            private:
            
                int id;
                int min_pulse;
                int max_pulse;
        
            public:
            
                PhidgetServo(ServoController *controller, int ID);
                virtual ~PhidgetServo();
                
                virtual void setActualPosition(double position);
                
            friend class PhidgetServoController;
        };

        /**
         * Abstract Phidget protocol handler.
         */
        class ProtocolHandler {
        
            public:
            
                /**
                 * Compose the command byte array to write into the phidget.
                 *
                 * @param positions Array of servo positions.
                 */
                virtual unsigned char *composeBuffer(int positions[]) = 0;
                
                /**
                 * Get the number of servos supported by this phidget.
                 */
                virtual int getServoCount() = 0;
        };
        
        class ProtocolHandler003 : public ProtocolHandler {
        
            private:
            
                /**
                 * Command buffer.
                 */
                unsigned char buffer[6];
        
            public:
            
                virtual unsigned char *composeBuffer(int servoPosition[]);
                virtual int getServoCount();
        };
        
        class ProtocolHandler004 : public ProtocolHandler {
        
            private:
            
                /**
                 * Command buffer.
                 */
                //unsigned char buffer[6];
        
            public:
            
                virtual unsigned char *composeBuffer(int servoPosition[]);
                virtual int getServoCount();
        };
    }

    class PhidgetServoController : protected ServoController {
    
        private:
        
            static phidget::ControllerDescriptor *modelTable[];
            
            /**
             * The USB context of the device found.
             *
             * May be NULL once in a while, if the device is disconnected.
             */
            phidget::UsbContext *thePhidgetServo;
            
            /**
             * The protocol handler instance.
             */
            phidget::ProtocolHandler *protocolHandler;
            
            /**
             * Physical servo representation.
             */
            Servo *servoSet;
            
            /**
             * Current servo position in device coordinates.
             */
            int *servoPosition;
            
            /**
             * Find the phidget with a given serial number.
             *
             * @param portName Serial number to look for.
             */
            phidget::UsbContext *findUSB(const char *portName);
            
            /**
             * Create a servo instance.
             *
             * @param id Servo ID.
             */
            Servo *createServo(int id);
            
            /**
             * Send the packet to the USB device.
             */
            void send();
            
        protected:

            virtual void checkInit();

        public:
        
            PhidgetServoController();
            virtual void init(const char *portName);
            virtual bool isConnected();
            
        friend class phidget::PhidgetServo;
    };
}

#endif
