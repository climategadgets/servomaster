## $Id: Makefile.am,v 1.6 2004-06-28 21:17:14 vtt Exp $

AUTOCONF_FILES = .servomaster.properties

jardir = ${prefix}/share/java
jar_DATA = @PACKAGE@-@VERSION@.jar

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
	    JAVA_HOME=@JAVA_HOME@ @ANT@ ; \
	else \
	    @ECHO@ "@PACKAGE@.jar is up to date." ; \
	fi

javadoc: build.xml

	@ if ${TEST} -z "${JAVADOC}" ; then ${ECHO} "javadoc was not found - rerun configure" ; exit 1; fi
	@ PATH=${PATH}:`${DIRNAME} ${JAVADOC}` JAVA_HOME=@JAVA_HOME@ @ANT@ javadocs
	
dist-hook:

	JAVA_HOME=@JAVA_HOME@ @ANT@ dist-copy-sources
	
clean-generic:

	@ ${RM} -rf *~ .*~ classes src ${AUTOCONF_FILES} ${jar_DATA} .servomaster.properties

@PACKAGE@-@VERSION@.jar: build

all: build

install-data-local:

#	JAVA_HOME=@JAVA_HOME@ @ANT@ install
