## $Id: Makefile.am,v 1.3 2001-09-03 08:24:35 vtt Exp $

SUBDIRS = src build

EXTRA_DIST = INSTALL-configure \
             INSTALL-Ant \
             index.html \
             docs/FAQ.html \
             docs/FT639.html \
             docs/future.html \
             docs/ideal_controller.html \
             docs/index.html \
             docs/SSC/ssc.pdf \
             docs/images/servos.jpg

CP = @CP@
RM = @RM@
FIND = @FIND@

clean-generic:

	${RM} -f `${FIND} -name "*~" -o -name ".*~" -o -name @PACKAGE@.jar`
	${RM} -rf ./docs/apidocs

javadoc:

	cd build && ${MAKE} javadoc
