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
