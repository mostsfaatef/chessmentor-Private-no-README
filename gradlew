#!/bin/sh
#
# Gradle wrapper launcher for ChessMentor (Gradle 8.7, JDK 17).
# Generated without running `gradle wrapper` (the sandbox blocks the
# Gradle daemon's loopback TCP, see README §10), so this is a compact
# equivalent of the standard script: it finds java and runs the
# wrapper bootstrap jar in gradle/wrapper/.
#
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if [ -n "$JAVA_HOME" ] ; then
    JAVA_EXE="$JAVA_HOME/bin/java"
else
    JAVA_EXE="$(command -v java)"
fi

if [ -z "$JAVA_EXE" ] || [ ! -x "$JAVA_EXE" ] ; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found." >&2
    echo "Install JDK 17 and set JAVA_HOME, e.g. export JAVA_HOME=/path/to/jdk-17" >&2
    exit 1
fi

# The Android SDK location: prefer local.properties, then ANDROID_HOME.
if [ -z "$ANDROID_HOME" ] && [ -f "$APP_HOME/local.properties" ] ; then
    ANDROID_HOME=$(grep -E '^sdk\.dir=' "$APP_HOME/local.properties" | cut -d= -f2-)
    export ANDROID_HOME
fi

exec "$JAVA_EXE" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain "$@"
