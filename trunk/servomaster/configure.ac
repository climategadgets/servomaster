dnl Process this file with autoconf to produce a configure script.

AC_REVISION($Id: configure.ac,v 1.7 2004-02-04 06:11:37 vtt Exp $)dnl

dnl This file ensures our --srcdir is right. Later it's also used to verify
dnl the acceptable version of Java compiler and the options specific to the
dnl versions.

AC_INIT
AC_CONFIG_SRCDIR([src/java/org/freehold/servomaster/device/model/Servo.java])

dnl I'd like it to read the configure-options to allow both the global set
dnl of options, as well as the local set.

GLOBAL_OPTIONS=/usr/local/share/servomaster/configure-options

if test -f "${GLOBAL_OPTIONS}" && test -z "${GLOBAL_OPTIONS_PRESENT}" ; then
    echo "Reading global options from ${GLOBAL_OPTIONS}"
    GLOBAL_OPTIONS_PRESENT="1" $0 `cat ${GLOBAL_OPTIONS}|tr "\n" " "` $*
    exit $?
fi

LOCAL_OPTIONS=`pwd`/configure-options

if test -f "${LOCAL_OPTIONS}" && test -z "${LOCAL_OPTIONS_PRESENT}" ; then
    echo "Reading local options from ${LOCAL_OPTIONS}"
    LOCAL_OPTIONS_PRESENT="1" $0 `cat ${LOCAL_OPTIONS}|tr "\n" " "` $*
    exit $?
fi

VERSION_MAJOR="0"
VERSION_MINOR="6"
PATCHLEVEL="2"

AC_SUBST(VERSION_MAJOR)dnl
AC_SUBST(VERSION_MINOR)dnl
AC_SUBST(PATCHLEVEL)dnl

PACKAGE="servomaster"
VERSION="${VERSION_MAJOR}.${VERSION_MINOR}p${PATCHLEVEL}"

AC_SUBST(PACKAGE)dnl
AC_SUBST(VERSION)dnl

AC_MSG_RESULT(Configuring ${PACKAGE}-${VERSION})

AC_PREFIX_DEFAULT(/usr/local/servomaster)dnl
AC_PREREQ(2.53)dnl
AC_CONFIG_AUX_DIR(src/scripts/install)dnl

AM_INIT_AUTOMAKE(${PACKAGE}, ${VERSION})dnl

AC_PROG_TEST
AC_PROG_TR
AC_PROG_FIND
AC_PROG_LN_S
AC_PATH_PROG_VERIFY(RM,rm,$PATH)dnl
AC_PATH_PROG_VERIFY(DIRNAME,dirname,$PATH)dnl
AC_PATH_PROG(RPM,rpm,,$PATH)dnl
AC_PATH_PROG(RPMBUILD,rpmbuild,,$PATH)dnl


AC_CANONICAL_HOST
case $host_os in
  *cygwin* ) CYGWIN=yes;;
         * ) CYGWIN=no;;
esac
dnl

if ${TEST} "${CYGWIN}" = "yes" ; then
    PATH_SEPARATOR="\\;"
else
    PATH_SEPARATOR=":"
fi

AC_SUBST(PATH_SEPARATOR)

AC_PROG_JAVATOOL(JAVA,java,[    Java runtime])
dnl AC_PROG_JAVATOOL(JAVAC,javac,[   Java compiler])
AC_PROG_JAVATOOL(JAVADOC,javadoc,[ Java documentation compiler])
dnl [You will not be able to create the Javadoc documentation])
dnl AC_PROG_JAVATOOL(JAR,jar,[     Java Archiver])

AC_PATH_JAVACLASS(ANT,/usr/share/java,org.apache.tools.ant.Main,[     Where to find the Jakarta Ant jar file (http://jakarta.apache.org/ant/)])
AC_PATH_JAVACLASS(JAVA_COMPILER,${JDK_HOME}/lib,sun.tools.javac.Main,[     Where to find the Jakarta javac jar file (http://jakarta.apache.org/)])
AC_PATH_JAVACLASS(XERCES,/usr/share/java,org.apache.xerces.parsers.DOMParser,[     Where to find the Xerces jar file (http://xml.apache.org/xerces-j/)])

FT_ENABLED=false

AC_MSG_CHECKING(if FT639 is enabled)

AC_ARG_ENABLE(ft639,
[  --enable-ft639          Enable support for FT639 controller],
[
    AC_MSG_RESULT(yes)
    FT_ENABLED=true
    
],
[
    AC_MSG_RESULT(no)
])

AC_SUBST(FT_ENABLED)

PHIDGET_ENABLED=false

AC_MSG_CHECKING(if phidget is enabled)

AC_ARG_ENABLE(phidget,
[  --enable-phidget        Enable support for Phidget controller family],
[
    AC_MSG_RESULT(yes)

    AC_ARG_WITH(jusb,
    [  --with-jusb             Specify the root directory of jUSB (http://jusb.sourceforge.net/)],
    [
        dnl Verify the library location
        
        AC_MSG_CHECKING(libjusb.so)
        
        if ${TEST} ! -f $withval/libjusb.so ; then
        
            AC_MSG_ERROR([jUSB shared library (libjusb.so) doesn't exist at ${withval}/libjusb.so])
        else
            JUSB_LIB=${withval}
            AC_SUBST(JUSB_LIB)
            AC_MSG_RESULT(${JUSB_LIB})
        fi
        
        dnl Verify the jar file location
        
        AC_PATH_JAVACLASS(JUSB,${withval},usb.core.Bus,[    Where to find the jUSB jar file (http://jusb.sourceforge.net/)])
        
    ])
    
    PHIDGET_ENABLED=true
],
[
    AC_MSG_RESULT(no)
])

AC_SUBST(PHIDGET_ENABLED)

if ${TEST} "${FT_ENABLED}" = "false" && ${TEST} "${PHIDGET_ENABLED}" = "false" ; then

    AC_MSG_RESULT()
    AC_MSG_RESULT([In order to build the project, at least one of --enable-ft639 and --enable-phidget])
    AC_MSG_RESULT([options have to be specified])
    AC_MSG_RESULT()
    
    AC_MSG_ERROR([Please specify --enable-ft639, --enable-phidget or both])
fi

TARGET_CLASSPATH=`cd ${srcdir} && pwd`/src/java

AC_PATH_EXTRA(ANT)
AC_PATH_EXTRA(JAVA_COMPILER)
AC_PATH_EXTRA(XERCES)

AC_SUBST(TARGET_CLASSPATH)dnl
AC_SUBST(JAVA_OPT)

AC_SUBST(EXEEXT)
AC_SUBST(OBJEXT)

AC_CONFIG_FILES([Makefile
	build/Makefile
	build/build.sh
	build/.servomaster.properties
	src/Makefile
	src/rpm/Makefile
	src/rpm/servomaster.spec
	src/scripts/Makefile
	src/scripts/run/Makefile
	src/scripts/run/servomaster_console
	src/scripts/test/Makefile
	src/scripts/test/servomaster_sync_check
	src/java/Makefile
])
AC_OUTPUT

if ${TEST} -z "$GLOBAL_OPTIONS_PRESENT" ; then
	AC_MSG_RESULT([You may want to create the ${GLOBAL_OPTIONS} file (see documentation)])
else
        AC_MSG_RESULT()
	AC_MSG_RESULT([Global options (${GLOBAL_OPTIONS}):])
	AC_MSG_RESULT()
	cat ${GLOBAL_OPTIONS}
	AC_MSG_RESULT()
fi

if ${TEST} -z "$LOCAL_OPTIONS_PRESENT" ; then
	AC_MSG_RESULT([You may want to create the ${LOCAL_OPTIONS} file (see documentation)])
else
        AC_MSG_RESULT()
	AC_MSG_RESULT([Local options (${LOCAL_OPTIONS}):])
	AC_MSG_RESULT()
	cat ${LOCAL_OPTIONS}
	AC_MSG_RESULT()
fi

AC_MSG_RESULT()

AC_MSG_RESULT(ServoMaster will be installed to:  ${prefix})
AC_MSG_RESULT(Configuration files are in:        ${prefix}/etc)
AC_MSG_RESULT(JDK_HOME and JAVA_HOME set to:     ${JDK_HOME})
AC_MSG_RESULT()
AC_MSG_RESULT(Build classpath set to:)

DISPLAY_CLASSPATH=`${ECHO} $TARGET_CLASSPATH|${TR} ":" "\n"`

AC_MSG_RESULT()

for ITEM in ${DISPLAY_CLASSPATH}; do
    AC_MSG_RESULT([        ${ITEM}])
done

AC_MSG_RESULT()

dnl Just to be sure the jar gets rebuilt in case we've changed the configuration

${RM} -f build/${PACKAGE}.jar
