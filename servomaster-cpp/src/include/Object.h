// $Id: Object.h,v 1.3 2002-09-13 08:37:53 vtt Exp $
#ifndef OBJECT_H
#define OBJECT_H

namespace servomaster {

    class Object {
    
        private:
        
            static long instanceCount;
    
        protected:
        
            bool initialized;
            virtual void checkInit();
            
        public:
        
            Object();
            virtual ~Object();
    };
}

#endif
