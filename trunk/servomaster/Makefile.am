## $Id: Makefile.am,v 1.1 2001-08-31 21:38:00 vtt Exp $

SUBDIRS = src build

EXTRA_DIST = INSTALL-configure INSTALL-Ant

CP = @CP@
RM = @RM@
FIND = @FIND@

clean-generic:

	${RM} -f `${FIND} -name "*~" -o -name ".*~" -o -name @PACKAGE@.jar`
	${RM} -rf ./docs/apidocs

javadoc:

	cd build && ${MAKE} javadoc
