## $Id: Makefile.am,v 1.4 2002-01-04 01:19:26 vtt Exp $

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
             docs/images/servos.jpg \
             etc/manifest

CP = @CP@
RM = @RM@
FIND = @FIND@

clean-generic:

	${RM} -f `${FIND} -name "*~" -o -name ".*~" -o -name @PACKAGE@.jar`
	${RM} -rf ./docs/apidocs

javadoc:

	cd build && ${MAKE} javadoc
