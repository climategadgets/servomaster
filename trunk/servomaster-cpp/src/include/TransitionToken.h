// $Id: TransitionToken.h,v 1.2 2002-09-13 08:37:53 vtt Exp $
#ifndef TRANSITIONTOKEN_H
#define TRANSITIONTOKEN_H

#include <Object.h>

namespace servomaster {

    class TransitionToken : Object {
    
        private:
        
            bool done;
            double position;
            
        public:
        
            void supply(double position);
            double consume();
            void stop();
    };
}

#endif