#include <ServoController.h>
#include <stddef.h>

namespace servomaster {

    ServoController::ServoController() : connected(false), disconnected(false), portName(NULL) {
    }
}
