// $Id: ServoController.cpp,v 1.4 2002-09-13 08:37:53 vtt Exp $
#include <ServoController.h>
#include <stddef.h>

namespace servomaster {

    ServoController::ServoController() : connected(false), disconnected(false), portName(NULL) {
    }
    
    const char *ServoController::getPort() {
    
        return (const char *)portName;
    }
}
