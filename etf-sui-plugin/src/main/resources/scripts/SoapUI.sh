#!/bin/sh
### ====================================================================== ###
##                                                                          ##
##  SoapUI Bootstrap Script                                                 ##
##                                                                          ##
### ====================================================================== ###

### $Id$ ###

ETFS_DIR=`pwd -P`
REQUIRED_VERSION=5.3.0


SOAPUI_HOME=/home/$USER/SmartBear/SoapUI-$REQUIRED_VERSION
# OS specific support (must be 'true' or 'false').
cygwin=false;
darwin=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;
    Darwin*)
        SOAPUI_HOME=/Applications/SoapUI-$REQUIRED_VERSION.app/Contents/java/app
        darwin=true
        ;;
esac
export SOAPUI_HOME

# Setup SOAPUI_HOME
#if [ -d $SOAPUI_HOME ]
#then
    # get the full path (without any relative bits)
#    SOAPUI_HOME=`cd $DIRNAME/..; pwd`
#fi

# SOAPUI_CLASSPATH=$ETFS_DIR/lib/*:$SOAPUI_HOME/bin/soapui-$REQUIRED_VERSION.jar:$SOAPUI_HOME/lib/*
SOAPUI_CLASSPATH=$SOAPUI_HOME/bin/soapui-$REQUIRED_VERSION.jar:$SOAPUI_HOME/lib/*
JFXRTPATH=`java -cp $SOAPUI_CLASSPATH com.eviware.soapui.tools.JfxrtLocator`
SOAPUI_CLASSPATH=$JFXRTPATH:$SOAPUI_CLASSPATH
export SOAPUI_CLASSPATH

JAVA_OPTS="-Xms128m -Xmx1024m -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -Dsoapui.properties=soapui.properties -Dgroovy.source.encoding=iso-8859-1 -Dsoapui.home=$SOAPUI_HOME/bin -splash:SoapUI-Spashscreen.png"

if $darwin
then
    JAVA_OPTS="$JAVA_OPTS -Dswing.crossplatformlaf=apple.laf.AquaLookAndFeel -Dapple.eawt.quitStrategy=CLOSE_ALL_WINDOWS"
fi

if [ $SOAPUI_HOME != "" ]
then
    mkdir $ETFS_DIR/logs
    JAVA_OPTS="$JAVA_OPTS -Dsoapui.ext.libraries=$SOAPUI_HOME/bin/ext"
    JAVA_OPTS="$JAVA_OPTS -Dsoapui.ext.listeners=$SOAPUI_HOME/bin/listeners"
    JAVA_OPTS="$JAVA_OPTS -Dsoapui.ext.actions=$SOAPUI_HOME/bin/actions"
    JAVA_OPTS="$JAVA_OPTS -Djava.library.path=$SOAPUI_HOME/bin"
    JAVA_OPTS="$JAVA_OPTS -Dsoapui.logroot=$ETFS_DIR/logs/"
	  JAVA_OPTS="$JAVA_OPTS -Dwsi.dir=$SOAPUI_HOME/wsi-test-tools"
# uncomment to disable browser component
#   JAVA_OPTS="$JAVA_OPTS -Dsoapui.browser.disabled=true"
fi
JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

export JAVA_OPTS

# For Cygwin, switch paths to Windows format before running java
if $cygwin
then
    SOAPUI_HOME=`cygpath --path --dos "$SOAPUI_HOME"`
    SOAPUI_CLASSPATH=`cygpath --path --dos "$SOAPUI_CLASSPATH"`
fi

cd $SOAPUI_HOME/bin
echo ================================
echo =
echo = SOAPUI_HOME = $SOAPUI_HOME
echo =
echo ================================

java $JAVA_OPTS -cp $SOAPUI_CLASSPATH com.eviware.soapui.SoapUI "$@"
cd $ETFS_DIR
