#include <ServoController.h>
#include <stddef.h>

namespace servomaster {

    ServoController::ServoController() : connected(false), disconnected(false), portName(NULL) {
    }
    
    const char *ServoController::getPort() {
    
        return (const char *)portName;
    }
}
