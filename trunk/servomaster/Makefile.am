## $Id: Makefile.am,v 1.5 2002-09-14 01:34:28 vtt Exp $

SUBDIRS = src build

EXTRA_DIST = INSTALL-configure \
             INSTALL-Ant \
             index.html \
             docs/FAQ.html \
             docs/FT639.html \
             docs/future.html \
             docs/ideal_controller.html \
             docs/index.html \
             docs/links.html \
             docs/quickstart.html \
             docs/smooth_transformer.html \
             docs/USB_Phidget.html \
             docs/credits.html \
             docs/quickstart_jar.html \
             docs/USB_Phidget4.html \
             docs/getinvolved.html \
             docs/news.html \
             docs/references.html \
             docs/legal.html \
             docs/images/servomaster_console.png \
             docs/images/servos.jpg \
             docs/images/phidget_console.png \
             docs/images/phidget_console.small.png \
             docs/images/ft639_console.small.png \
             docs/images/PhidgetServo_v4.0.jpg \
             etc/manifest

CP = @CP@
RM = @RM@
FIND = @FIND@

clean-generic:

	${RM} -f `${FIND} -name "*~" -o -name ".*~" -o -name @PACKAGE@.jar`
	${RM} -rf ./docs/apidocs

javadoc:

	cd build && ${MAKE} javadoc
