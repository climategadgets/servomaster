## $Id: Makefile.am,v 1.1 2002-09-12 00:00:09 vtt Exp $

SUBDIRS = src
EXTRA_DIST = bootstrap indent.rules src/scripts/install/*

clean-generic:

	@RM@ -f `@FIND@ -name "*~" -o -name ".*~"`

indent:

	@ if @TEST@ -z "@INDENT@" ; then \
	    @ECHO@ "indent was not found, install and rerun ./configure"; \
	    @RM@ -f config.cache; \
	fi

	@ for FILE in `@FIND@ . -name "*.h" -o -name "*.c" -o -name "*.cc"` ; do \
	    @INDENT@ --ignore-profile --standard-output `@CAT@ ./indent.rules` `@CAT@ ./indent.defs` $$FILE > $$FILE.indent; \
	    if @TEST@ -n "`@DIFF@ $$FILE $$FILE.indent`" ; then \
	        if @TEST@ ! -s $$FILE.indent ; then \
	            @ECHO@ "@INDENT@ run resulted in an empty file: $$FILE.indent; most probably the options in ./indent.rules are wrong"; \
	            @RM@ $$FILE.indent; \
	            exit 1; \
	        fi; \
	        @MV@ $$FILE.indent $$FILE; \
	    else \
	        @RM@ $$FILE.indent; \
	    fi; \
	done

indent-local:

	@ if @TEST@ -z "@INDENT@" ; then \
	    @ECHO@ "indent was not found, install and rerun ./configure"; \
	    @RM@ -f config.cache; \
	fi

	@ for FILE in `@FIND@ . -name "*.h" -o -name "*.c" -o -name "*.cc"` ; do \
	    @INDENT@ --standard-output `@CAT@ ./indent.defs` $$FILE > $$FILE.indent; \
	    if @TEST@ -n "`@DIFF@ $$FILE $$FILE.indent`" ; then \
	        if @TEST@ ! -s $$FILE.indent ; then \
	            @ECHO@ "@INDENT@ run resulted in an empty file: $$FILE.indent; most probably the options in ./indent.rules are wrong"; \
	            @RM@ $$FILE.indent; \
	            exit 1; \
	        fi; \
	        @MV@ $$FILE.indent $$FILE; \
	    else \
	        @RM@ $$FILE.indent; \
	    fi; \
	done

rpm: dist

	@ if @TEST@ -z "@RPM@" ; then \
	    @ECHO@ "RPM binary not found, can't build RPM package"; \
	    exit 1; \
	fi
	
	# VT: FIXME: It may be a good idea to check other dependencies
	# required for a *complete* RPM build, such as Doxygen, and refuse
	# to build the RPM, unless something like --enable-lazy-deps is
	# provided
	
	cd src/rpm && ${MAKE} @PACKAGE@.spec
	@LN_S@ -f `pwd`/src/rpm/@PACKAGE@.spec /usr/src/redhat/SPECS/@PACKAGE@.spec
	@LN_S@ -f `pwd`/@PACKAGE@-@VERSION@.tar.gz /usr/src/redhat/SOURCES/@PACKAGE@-@VERSION@.tar.gz
	cd /usr/src/redhat/SPECS && @RPM@ -ba @PACKAGE@.spec
