## $Id: Makefile.am,v 1.4 2002-12-12 01:46:52 vtt Exp $

AUTOCONF_FILES = build.sh

jardir = ${prefix}/lib
jar_DATA = @PACKAGE@.jar

# These files may be used to build the package even without
# autoconf/automake present

EXTRA_DIST = ${AUTOCONF_FILES} build.xml

RM = @RM@
DIRNAME = @DIRNAME@
JAVADOC = @JAVADOC@
TEST = @TEST@

.PHONY: @PACKAGE@.jar

.servomaster.properties: .servomaster.properties.in

build: ${AUTOCONF_FILES} .servomaster.properties

	@ if @TEST@ ! -f @PACKAGE@.jar \
	  || @TEST@ -n "`@FIND@ ../src/java -name "*.java" -newer @PACKAGE@.jar`" \
	  || @TEST@ -n "`@FIND@ . -name "build.xml" -newer @PACKAGE@.jar`" ; then \
	    sh ./build.sh ; \
	else \
	    @ECHO@ "@PACKAGE@.jar is up to date." ; \
	fi

javadoc: build.xml build.sh

	@ if ${TEST} -z "${JAVADOC}" ; then ${ECHO} "javadoc was not found - rerun configure" ; exit 1; fi
	@ PATH=${PATH}:`${DIRNAME} ${JAVADOC}` sh ./build.sh javadocs
	
dist-hook:

	sh ./build.sh dist-copy-sources
	
clean-generic:

	@ ${RM} -rf *~ .*~ classes src ${AUTOCONF_FILES} ${jar_DATA} .servomaster.properties

@PACKAGE@.jar: build

all: build

install-data-local:

#	sh ./build.sh install
