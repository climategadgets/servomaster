// $Id: Servo.h,v 1.5 2002-09-14 03:32:56 vtt Exp $
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
        
            /**
             * The requested position.
             */
            double position;
            
            /**
             * Whether the servo is enabled or not.
             */
            bool enabled;
            
            /**
             * The controller this servo belongs to.
             */
            ServoController *servoController;
            
            /**
             * The chained servo.
             */
            Servo *target;
            
            /**
             * Transition controller, if any.
             *
             * If this field is NULL, the servo commands will be carried out
             * immediately, otherwise, the transition controller will
             * execute the actual servo movement as it sees fit.
             */
            TransitionController *transitionController;
            
            /**
             * The driver for the current transition, if any.
             */
            servo::TransitionDriver *transitionDriver;
            
        protected:
        
            /**
             * Actual servo position.
             */
            double actualPosition;
            
            /**
             * Check if the requested position is between 0.0 and 1.0.
             */
            void checkPosition(double position);
    
            /**
             * Set the servo position immediately.
             *
             * This method is called by the transition controller.
             *
             * @param position Desired position.
             */
            virtual void setActualPosition(double position) = 0;

        public:
        
            Servo(ServoController *servoController, Servo *target);
            virtual ~Servo();
        
            ServoController *getController();
            
            /**
             * Attach the transition controller.
             *
             * @param transitionController The transition controller to use
             * from now on.
             *
             * @return The old transition controller. You must explicitly
             * delete it.
             */
            TransitionController *attach(TransitionController *transitionController);
            
            /**
             * @return Currently used transition controller.
             */
            TransitionController *getTransitionController();
            
            /**
             * @return Requested position.
             */
            double getPosition();
            
            /**
             * @return Actual position.
             */
            double getActualPosition();
            
            /**
             * @return The servo name.
             */
            const char *getName();
            
            /**
             * @return The chained servo.
             */
            Servo *getTarget();
            
            /**
             * Set the servo into enabled or disabled state.
             */
            void setEnabled(bool enabled);
            
            /**
             * Request the servo to move into a given position.
             *
             * Depending on whether the kind of transition controller is
             * attached, and/or whether it is attached at all, the servo
             * will move immediately, or the transition to the new position
             * will occur.
             *
             * @param position Desired position.
             */
            void setPosition(double position);
            
            
            /**
             * Set the servo range.
             * 
             * Some controllers allow to select the servo range between 0-90
             * and 0-180 degrees.
             */
            void setRange(int range);
    };
}

#endif
