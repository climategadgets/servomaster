#ifndef SERVO_H
#define SERVO_H

#include <Object.h>
#include <ServoController.h>
#include <TransitionController.h>

namespace servomaster {

    class Servo;
    namespace servo {
    
        class TransitionDriver {
        
            private:
            
                Servo *target;
                double position;
        
            public:
            
                TransitionDriver(Servo *target, double position);
                void start();
                void stop();
        
        };
    }

    class ServoController;
    class TransitionController;
    class Servo : Object {
    
        private:
        
            double position;
            bool enabled;
            ServoController *servoController;
            Servo *target;
            TransitionController *transitionController;
            servo::TransitionDriver *transitionDriver;
            
        protected:
        
            double actualPosition;
            void checkPosition(double position);
    
        public:
        
            Servo(ServoController *servoController, Servo *target);
            virtual ~Servo();
        
            // ServoMetaData *getMetaData();
            ServoController *getController();
            void attach(TransitionController *transitionController);
            TransitionController *getTransitionController();
            double getPosition();
            double getActualPosition();
            const char *getName();
            Servo *getTarget();
            void setEnabled(bool enabled);
            void setPosition(double position);
            virtual void setActualPosition(double position) = 0;
            void setRange(int range);
    };
    
}

#endif
