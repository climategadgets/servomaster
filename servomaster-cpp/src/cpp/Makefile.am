# $Id: Makefile.am,v 1.5 2003-09-03 06:19:36 vtt Exp $

MODEL_SOURCE = Object.cpp \
               Servo.cpp \
               ServoController.cpp \
               TransitionController.cpp \
               TransitionToken.cpp

#               DisconnectListener.cpp \
#               Meta.cpp \
#               ProblemListener.cpp \
#               ServoControllerListener.cpp \
#               ServoControllerMetaData.cpp \
#               ServoListener.cpp \
#               ServoMetaData.cpp \
#               SilentDevice.cpp

PHIDGET_SOURCE = PhidgetServoController.cpp

lib_LTLIBRARIES = libservomaster.la

libservomaster_la_SOURCES = ${MODEL_SOURCE} ${PHIDGET_SOURCE}

bin_PROGRAMS = servomaster-console

servomaster_console_SOURCES = servomaster_console.cpp

servomaster_console_LDADD = libservomaster.la
servomaster_console_DEPENDENCIES = libservomaster.la

CXXFLAGS = -I../include -fexceptions -Wall -W -Wundef -Wshadow -Wpointer-arith -Wcast-qual -Wcast-align -Wconversion

clean-generic:

	@RM@ -f *~ .*~
