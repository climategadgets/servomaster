## $Id: Makefile.am,v 1.10 2004-10-13 01:25:00 vtt Exp $

SUBDIRS = src build lib
RPMMACROS = ${HOME}/.rpmmacros
RPMDIR = ${HOME}/.rpm

EXTRA_DIST = INSTALL-configure \
             INSTALL-Ant \
             index.html \
             docs/.cvsignore \
             docs/FAQ.html \
             docs/FT639.html \
             docs/USB_Phidget.html \
             docs/USB_Phidget4.html \
             docs/build.html \
             docs/credits.html \
             docs/getinvolved.html \
             docs/future.html \
             docs/ideal_controller.html \
             docs/index.html \
             docs/legal.html \
             docs/links.html \
             docs/news.html \
             docs/references.html \
             docs/slls466.pdf \
             docs/smooth_transformer.html \
             docs/images/advanced_phidget_console.png \
             docs/images/advanced_phidget_console.small.png \
             docs/images/servomaster_console.png \
             docs/images/servos.jpg \
             docs/images/phidget_console.png \
             docs/images/phidget_console.small.png \
             docs/images/ft639_console.small.png \
             docs/images/PhidgetServo_v4.0.jpg \
             etc/manifest \
             meta/controller/bandwidth/index.html \
             meta/controller/firmware/revision/index.html \
             meta/controller/maxservos/index.html \
             meta/controller/precision/index.html \
             meta/controller/protocol/serial/index.html \
             meta/controller/protocol/USB/index.html \
             meta/controller/silent/index.html \
             meta/controller/transition/index.html \
             meta/index.html \
             meta/manufacturer/model/index.html \
             meta/manufacturer/name/index.html \
             meta/manufacturer/URL/index.html \
             meta/servo/precision/index.html \
             meta/servo/range/max/index.html \
             meta/servo/range/min/index.html \
             meta/servo/range/units/index.html
             
CP = @CP@
RM = @RM@
FIND = @FIND@

clean-generic:

	${RM} -f `${FIND} -name "*~" -o -name ".*~" -o -name @PACKAGE@.jar`
	${RM} -rf ./docs/apidocs

javadoc:

	cd build && ${MAKE} javadoc

rpm: dist

	@ if @TEST@ -z "@RPMBUILD@" ; then \
	    @ECHO@ "RPMBUILD binary not found, can't build RPM package"; \
	    exit 1; \
	fi
	
	if @TEST@ ! -f "${RPMMACROS}" ; then \
	    @ECHO@ "%_topdir ${RPMDIR}" > ${RPMMACROS}; \
	    mkdir -p ${RPMDIR}/SOURCES \
	             ${RPMDIR}/SPECS \
	             ${RPMDIR}/BUILD \
	             ${RPMDIR}/RPMS/i386 \
	             ${RPMDIR}/SRPMS ; \
	fi
	
	cd src/rpm && ${MAKE} @PACKAGE@.spec
	@LN_S@ -f `pwd`/src/rpm/@PACKAGE@.spec ${RPMDIR}/SPECS/@PACKAGE@.spec
	@LN_S@ -f `pwd`/@PACKAGE@-@VERSION@.tar.gz ${RPMDIR}/SOURCES/@PACKAGE@-@VERSION@.tar.gz
	cd ${RPMDIR}/SPECS && @RPMBUILD@ -ba @PACKAGE@.spec
