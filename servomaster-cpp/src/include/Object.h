#ifndef OBJECT_H
#define OBJECT_H

namespace servomaster {

    class Object {
    
        protected:
        
            bool initialized;
            virtual void checkInit();
            
        public:
        
            Object();
    };
}

#endif
