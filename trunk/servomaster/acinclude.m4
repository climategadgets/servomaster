dnl This file contains autoconf macros used by jukebox. -*-sh-*-
dnl 
dnl Copyright (C) 1997 Free Software Foundation, Inc.
dnl Copyright (C) 1999-2000 Vadim Tkachenko <vt@freehold.crocodile.org> 
dnl CHECK_GNU_MAKE thanks to John Darrington <j.darrington@elvis.murdoch.edu.au>
dnl
dnl Authoritative source URL for this file is <http://jukebox4.sourceforge.net/acinclude.m4>

dnl These are the top-level macros provided:
dnl 
dnl AC_PATH_JDK		Finds the JDK or accepts the location argument.
dnl AC_PROG_JAVATOOL	Finds the JDK tool or accepts the location argument.
dnl AC_PATH_JAVACLASS	Finds the jar file or directory with the classes or
dnl			accepts the location argument.
dnl
dnl Also, there are others, but I wouldn't recommend to use them just for
dnl consistency sake. If you want to use them, analyze the source.

dnl Find the 'false program, I'll need it as a substitute later
dnl Result goes in FALSE
AC_DEFUN(AC_PROG_FALSE,
[
    AC_PATH_PROG_VERIFY(FALSE,false,$PATH)
])

dnl Ditto for echo
AC_DEFUN(AC_PROG_ECHO,
[
    AC_PATH_PROG_VERIFY(ECHO,echo,$PATH)
])

dnl Ditto for test
AC_DEFUN(AC_PROG_TEST,
[
    AC_PATH_PROG_VERIFY(TEST,test,$PATH)
])

dnl Ditto for grep
AC_DEFUN(AC_PROG_GREP,
[
    AC_PATH_PROG_VERIFY(GREP,grep,$PATH)
])

dnl Ditto for find
AC_DEFUN(AC_PROG_FIND,
[
    AC_PATH_PROG_VERIFY(FIND,find,$PATH)
])

dnl Ditto for cut
AC_DEFUN(AC_PROG_CUT,
[
    AC_PATH_PROG_VERIFY(CUT,cut,$PATH)
])

dnl Ditto for tr
AC_DEFUN(AC_PROG_TR,
[
    AC_PATH_PROG_VERIFY(TR,tr,$PATH)
])

AC_DEFUN(AC_PROG_JAR,
[
    AC_PROG_JAVATOOL(JAR,jar,[     Java Archiver])
])

dnl Find the JDK
dnl Results go in JDK_HOME
dnl Also sets JAVA_PLATFORM to 1 for 1.1 and to 2 for 1.2
dnl See also AC_WITH_PLATFORM

AC_DEFUN(AC_PATH_JDK,
[
    if ${TEST} "${CYGWIN}" = "yes" ; then
        AC_MSG_WARN(It will take quite a while to find the JDK home on Cygwin)
    fi

    AC_MSG_CHECKING(for JDK home)

    dnl The order is: --with-jdk first, environment second, guessed value third.
    
    dnl This is a safe default. Could screw up on the security features, but
    dnl oh well, this is what --with-java2 is for.
    
    JAVA_PLATFORM="1"
    JDK_HOME_FOUND=""

    AC_ARG_WITH(jdk-home,
    [  --with-jdk-home=DIR     Where is your JDK root directory.],
    [
	JDK_HOME=${withval}
	
	if ${TEST} ! -d "${JDK_HOME}" ; then
	    AC_MSG_ERROR(Not a directory: ${JDK_HOME})
	fi
	
	AC_MSG_RESULT(${JDK_HOME})
	
    ],
    [
        if ${TEST} -n "${JDK_HOME}" ; then
        
            dnl Well, I didn't think of trying to recognize the platform if
            dnl it was specified in the environment...
            
            AC_MSG_RESULT([Using environment: ${JDK_HOME}, you may want to specify --with-java2 if required])
        fi
    ])

    if ${TEST} -z "${JDK_HOME}" ; then

        for JDK_PREFIX in \
    	    / \
    	    /usr \
    	    /usr/java \
    	    /usr/local \
    	    /usr/lib \
    	    /usr/local/lib \
            /opt
        do
    	for VARIANT in \
    	    j2sdk \
    	    java \
    	    jdk 
    	do
    	    
    	    for JAVA_VERSION in \
    	        1.4.1_01 \
    	        1.4.1 \
    	        1.4.0 \
    	        1.3.1 \
    	        1.3 \
            	1.2.2 \
            	1.2.1 \
            	1.2 \
                1.1.8 \
            	1.1.7 \
            	1.1.6 \
            	1.1.5 \
            	1.1.4 \
            	1.1.3 \
            	1.1.2 \
            	1.1.1 \
            	1.1
                do
                    if ${TEST} -d "${JDK_PREFIX}/${VARIANT}${JAVA_VERSION}/bin" \
                    && ${TEST} -d "${JDK_PREFIX}/${VARIANT}${JAVA_VERSION}/include" ; then

            	    JDK_HOME="${JDK_PREFIX}/${VARIANT}${JAVA_VERSION}"
		    AC_MSG_RESULT(${JDK_HOME})
            	    
                    dnl Let's try to guess the Java version from the pathname, to save an expensive check
                    AC_MSG_CHECKING(Java platform)

                    JAVA_PLATFORM=`echo "$JAVA_VERSION"|cut -c 3`

                    case ${JAVA_PLATFORM} in

                        1)
                            AC_MSG_RESULT(guess 1.1.x)
                            ;;
                            
                        2)
                            AC_MSG_RESULT(guess Java 2)
                            ;;

                        3)
                            AC_MSG_RESULT([guess Java 2 (1.3)])
                            ;;
                            
                        4)
                        
                            AC_MSG_RESULT([guess Java 2 (1.4)])
                            ;;

                        *)
                            AC_MSG_WARN([Undefined, presumed 1.1.x, you better check it and/or rename the directory])
                            JAVA_PLATFORM=1
                            ;;
                    esac
                    AC_SUBST(JAVA_PLATFORM)
                    JDK_HOME_FOUND="yes"

            	    break
            	fi
                  
                  if ${TEST} -n "${JDK_HOME_FOUND}" ; then
                      break
                  fi
                done
                if ${TEST} -n "${JDK_HOME_FOUND}" ; then
                    break
                fi
    	    done
          if ${TEST} -n "${JDK_HOME_FOUND}" ; then
              break
          fi
        done
    else
    
        AC_MSG_CHECKING(Java platform)

        AC_ARG_WITH(java2,
        [  --with-java2            Force the Java 2 platorm],
        [
            dnl This requires a bit of tweaking to be handled properly, but
            dnl the default is good enough
            
            if ${TEST} "${JAVA_PLATFORM}" = "3" ; then
                AC_MSG_RESULT([with all due respect, I will not downgrade])
            else
                JAVA_PLATFORM="2"
                AC_SUBST(JAVA_PLATFORM)
                AC_MSG_RESULT(forced Java ${JAVA_PLATFORM})
            fi
        ])

        

    fi

if ${TEST} -z "${JDK_HOME}" ; then

    AC_MSG_ERROR(JDK home not found)
fi

JAVA_HOME=${JDK_HOME}
AC_SUBST(JAVA_HOME)
AC_SUBST(JDK_HOME)
AC_PROVIDE([$0])
])

dnl Find the  rmic program (so far useful only for EJBoss
dnl (http://www.ejboss.org/), but preserved because doesn't break anything and
dnl allows this file to be reused for Jserv, ECS, Jukebox and EJBoss.
dnl 
dnl I guess I'd work on this one later. I don't like it, it's not generic (VT).

dnl Result goes in RMIC (bare location) and RMICX (with classpath and flag
dnl adjustments)

dnl The order is: --with-rmic first, environment second, guessed value third.

AC_DEFUN(AC_PROG_RMIC,
[
    AC_REQUIRE([AC_PATH_JDK])
    AC_REQUIRE([AC_PROG_FALSE])
    AC_MSG_CHECKING(rmic binary)

    AC_ARG_WITH(rmic,
    [  --with-rmic=prog        Java RMI compiler you want to use, if not the one from JDK],
    [
        if ${TEST} "$withval" = "yes" || ${TEST} "$withval" = "no" ; then
            AC_MSG_ERROR(You must specify the rmic binary as the parameter for --with-rmic)
        fi
        
        if ${TEST} ! -x "$withval" ; then
            AC_MSG_ERROR(Bad value for --with-rmic: $withval)
        fi
    ],
    [
        RMIC="${JDK_HOME}/bin/rmic"
        if ${TEST} ! -x "${RMIC}" ; then
            AC_MSG_ERROR(rmic binary not found in ${JDK_HOME})
        fi
    ])
    
    AC_MSG_RESULT(${RMIC})
    AC_SUBST(RMIC)
    AC_PROVIDE([$0])

    if ${TEST} "${JAVA_PLATFORM}" = "2" ; then
    	RMICX="${RMIC} -classpath \${TARGET_CLASSPATH}${PATH_SEPARATOR}${JDK_HOME}/jre/lib/rt.jar"
    else
    	RMICX="CLASSPATH=\${CLASSPATH}${PATH_SEPARATOR}\${TARGET_CLASSPATH}${PATH_SEPARATOR}${JDK_HOME}/lib/classes.zip ${RMIC}"
    fi

    AC_SUBST(RMICX)
])

dnl Locate the Java tool within a JDK tree or by --with-* option.

dnl First parameter is the name of the variable it goes to
dnl Second parameter is the name of the binary to look up
dnl Third parameter is the --help message (pay attention to alignment)
dnl If the forth parameter is present and the binary is not found, just
dnl substitute it with $FALSE and display a warning message instead of bailing
dnl out.

dnl Results go to $1 (bare location) and $1X (with classpath and flag
dnl adjustments).

AC_DEFUN(AC_PROG_JAVATOOL,
[
    AC_REQUIRE([AC_PATH_JDK])
    AC_REQUIRE([AC_PROG_FALSE])
    AC_MSG_CHECKING($2)

    AC_ARG_WITH($2,
    [  --with-$2=prog    $3 you want to use, if not the one from JDK],
    [
        if ${TEST} "$withval" = "yes" || ${TEST} "$withval" = "no" ; then
            AC_MSG_ERROR(You must specify the $2 binary as the parameter for --with-$2)
        fi
        
        if ${TEST} ! -x "$withval" ; then
            AC_MSG_ERROR(Bad value for --with-$2: $withval)
        fi
    ],
    [
        $1="${JDK_HOME}/bin/$2"
        if ${TEST} ! -x "${$1}" ; then
            if ${TEST} -n "$4" ; then
                AC_MSG_WARN($4)
                $1=${FALSE}
            else
                AC_MSG_ERROR($2 binary not found in ${JDK_HOME})
            fi
        fi
    ])
    
    AC_MSG_RESULT(${$1})
    AC_SUBST($1)
    
    AC_PROVIDE($1)
    
    dnl VT: This ugly bypass is here because autoconf complains about its
    dnl bug. In the future, I'll be checking it once in a while
    
    
    OPTION=$1
    OPTION="${OPTION}_OPT"

    if ${TEST} "${JAVA_PLATFORM}" = "2" ; then
    	$1X="${$1} \${$OPTION} -classpath \${TARGET_CLASSPATH}"
    else
    	$1X="CLASSPATH=\${CLASSPATH}${PATH_SEPARATOR}\${TARGET_CLASSPATH}${PATH_SEPARATOR}${JDK_HOME}/lib/classes.zip ${$1} \${$OPTION}"
    fi

    AC_SUBST($1X)
])

dnl Find the Java class set.

dnl The first parameter is the name of the variable it goes to
dnl The second parameter is a guessed location basename
dnl The third parameter defines a name of the class that has to be present
dnl in the target entity
dnl The forth parameter is a --help message
dnl If the fifth parameter is not empty, this package is optional.
dnl The output goes into $1_CLASSES

AC_DEFUN(AC_PATH_JAVACLASS,
[
    AC_REQUIRE([AC_PROG_TEST])
    AC_REQUIRE([AC_PROG_ECHO])
    AC_REQUIRE([AC_PROG_CUT])
    AC_REQUIRE([AC_PROG_GREP])
    AC_MSG_CHECKING($1)
    
    dnl Find out if we have a parameter
    
    JAVACLASS_DISABLED=""
    
    AC_ARG_WITH($1,
    [  --with-$1=DIR/JAR $4],
    [
        $1_CLASSES=$withval

        dnl Find out if it is not disabled

        
        if ${TEST} "$withval" = "no" ; then
        
            AC_MSG_RESULT(disabled)
            JAVACLASS_DISABLED="yes"
            $1_CLASSES=""
        fi
    ],
    [

        dnl If the name starts with /, consider it absolute and don't touch,
        dnl if it contains /, don't touch it either, otherwise treat it as
        dnl /usr/local/${file}
        
        ABSOLUTE=`${ECHO} $2|${CUT} -c 1|${GREP} /`
        
        if ${TEST} -n "${ABSOLUTE}" ; then
        
            # It is an absolute name, don't touch it
            
            $1_CLASSES=$2
            
        else
        
            CONTAINS_SLASH=`${ECHO} $2|${GREP} /`
            
            if ${TEST} -n "${CONTAINS_SLASH}" ; then
            
                # If it contains /, don't touch it
                
                $1_CLASSES=$2
                
            else
            
                $1_CLASSES="/usr/local/$2"
            fi
        fi
    ])
    
    if ${TEST} "${JAVACLASS_DISABLED}" != "yes" ; then
    
        dnl Find out if it exists at all
            
        if ${TEST} ! -e "${$1_CLASSES}" ; then
            if ${TEST} -z "$5" ; then
                AC_MSG_ERROR(Does not exist: '${$1_CLASSES}')
            else
                AC_MSG_RESULT(not found in ${$1_CLASSES})
            fi
        else

            dnl Transform the class name into the path name
            
            CLASS="`${ECHO} $3|${TR} "." "/"`.class"
            
            dnl Find out what it is
            
            if ${TEST} -d "${$1_CLASSES}" ; then
            
                dnl OK, so this is a directory. Try to find the class,
                dnl giving the preference to the jar
                
                AC_PATH_SEARCHJAR($1,${$1_CLASSES},${CLASS})
                
                if ${TEST} -z "${$1}" ; then

                    AC_PATH_SEARCHCLASS($1,${$1_CLASSES},${CLASS})
                    
                    if ${TEST} -z "${$1}" ; then
                        if ${TEST} -z "$5" ; then
                            AC_MSG_ERROR(no $3 class or jar with it in ${$1_CLASSES})
                        else
                            AC_MSG_RESULT(not found)
                        fi
                    fi
                fi
            else
            
                dnl OK, so this is a jar file
                
                AC_PATH_VERIFYJAR($1,${$1_CLASSES},${CLASS})
                
                if ${TEST} -z "${$1}" ; then
                    AC_MSG_ERROR($3 not found in ${$1_CLASSES})
                fi
            fi
        fi
        
        $1_CLASSES=${$1}
        
        if ${TEST} -n "${$1_CLASSES}" ; then
            AC_MSG_RESULT(${$1_CLASSES})
        fi
        
        AC_SUBST($1_CLASSES)
        AC_PROVIDE($1_CLASSES)
    
    fi
    
    dnl It appears that it's not possible to use the substitution in AM_CONDITIONAL,
    dnl so by now it's required to use it in configure.in, unfortunately.
    
    dnl AM_CONDITIONAL($1,${TEST} -n "${$1_CLASSES}")
])

dnl This one verifies if the $3 class exists in the $2 jar file and places
dnl the $2 in $1 if it is, otherwise sets it to empty string.
 
AC_DEFUN(AC_PATH_VERIFYJAR,
[
    AC_REQUIRE([AC_PROG_JAR])
    AC_REQUIRE([AC_PROG_TEST])
    
    dnl You may want to call it before, just to make the output look good
    
    $1=`${JAR} -tvf $2 2>&1|grep $3`
    
    if ${TEST} -n "${$1}" ; then
    	$1=$2
    else
        $1=""
    fi
    AC_SUBST($1)
])

dnl Find a $3 class file in the $2 directory.
dnl If the directory is a classpath root, return it in $1.
dnl If it is not, but the class is there, bail out.
dnl If it doesn't contain the class file at all, return the empty string.

AC_DEFUN(AC_PATH_SEARCHCLASS,
[

    AC_REQUIRE([AC_PROG_FIND])
    AC_REQUIRE([AC_PROG_GREP])
    AC_REQUIRE([AC_PROG_ECHO])
    AC_REQUIRE([AC_PROG_TEST])
    AC_REQUIRE([AC_PROG_CUT])
    
    PRESENT=`(cd $2 && ${FIND} . -name "*.class"|${GREP} "$3"|${CUT} -c 3-)`
    
    if ${TEST} -n "${PRESENT}" ; then
    
        PRESENT=`${ECHO} ${PRESENT}|${GREP} -x "${CLASS}"`
        
        if ${TEST} -z "${PRESENT}" ; then

            dnl OK, here's a misajustment - let's try to fix it later by
            dnl calculating the length difference, so far - error
            
            AC_MSG_ERROR([$2 is not a classpath root for $3 - adjust it])
        else
            $1=$2
        fi
    fi
    
    AC_SUBST($1)
])

dnl Find all the jar and zip files below $2 and check them for $3 class until found.
dnl Bail out if there's no file in there.
dnl If the jar/zip is found, return it in $1.

dnl (VT: looks like AC_PATH_VERIFYJAR, may be just use it?)

AC_DEFUN(AC_PATH_SEARCHJAR,
[
    AC_REQUIRE([AC_PROG_FIND])
    AC_REQUIRE([AC_PROG_TR])
    AC_REQUIRE([AC_PROG_TEST])
    
    JARS=`${FIND} $2/ -name "*.jar" -o -name "*.zip"|${TR} "\n" " "`
    
    if ${TEST} -n "$JARS" ; then
        for JARFILE in ${JARS}; do
dnl            AC_MSG_CHECKING('$JARFILE' for $3)
            PRESENT=`${JAR} -tf ${JARFILE} 2>&1|grep $3`
            
dnl            AC_MSG_RESULT(Got '${PRESENT}')
            
            if ${TEST} -n "${PRESENT}" ; then
                $1=${JARFILE}
                AC_SUBST($1)
                break
            fi
        done
    fi
])

dnl This one displays one-line summary on the optional jar/class component,
dnl if the correspondend environment variable is set.
dnl
dnl First parameter is the environment variable name, without _CLASSES appended.

AC_DEFUN(AC_REPORT_OPTIONAL, [

    if ${TEST} -n "${$1_CLASSES}" ; then
        AC_MSG_RESULT($1 (optional) used: ${$1_CLASSES})
    else
        AC_MSG_RESULT($1: (optional) not used)
    fi
])

dnl This one displays one-line summary on the mandatory jar/class component,
dnl if the correspondend environment variable is set.
dnl
dnl First parameter is the environment variable name, without _CLASSES appended.

AC_DEFUN(AC_REPORT_MANDATORY, [

    if ${TEST} -n "${$1_CLASSES}" ; then
        AC_MSG_RESULT($1 used: ${$1_CLASSES})
    else
        AC_MSG_ERROR($1: how come I see this here?)
    fi
])

AC_DEFUN(AC_PATH_EXTRA,
[
    if ${TEST} -n "${$1_CLASSES}" ; then
        TARGET_CLASSPATH="${TARGET_CLASSPATH}${PATH_SEPARATOR}${$1_CLASSES}"
    fi
])

AC_DEFUN(AC_PATH_PROG_VERIFY,
[

    dnl $TEST must be defined before running this. The case when $TEST is
    dnl not defined yet, but this macro is used to find it, is rather
    dnl tricky, but rather rare ;)
    
    AC_PATH_PROG($1,$2,$3)
    
    dnl Can't use -x here 'cause 4.3BSD doesn't have it

    if ${TEST} ! -f "${$1}" ; then
        AC_MSG_ERROR([$2 doesn't exist])
    fi
])

dnl Search all the common names for GNU make

AC_DEFUN(CHECK_GNU_MAKE,
[
    AC_CACHE_CHECK( for GNU make,_cv_gnu_make_command,_cv_gnu_make_command='' ;

        for MK in "$MAKE" make gmake gnumake ; do
            RESULT=`${MK} --version 2>/dev/null|${GREP} GNU`;
            if  ${TEST} -n "${RESULT}" ;  then
                _cv_gnu_make_command=$MK ;
                break;
            fi
        done ;
    ) ;

dnl If there was a GNU version, then set @ifGNUmake@ to the empty string, '#' otherwise

    if ${TEST}  -n "$_cv_gnu_make_command"  ; then
        ifGNUmake='';
    else
        ifGNUmake='#';
    fi
    AC_SUBST(ifGNUmake)
] )
