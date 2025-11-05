#!/usr/bin/env sh

set -e

DIR="$(dirname "$0")"
APP_BASE_NAME="Gradle"
APP_HOME=$(cd "$DIR" && pwd)

if [ -z "$JAVA_HOME" ]; then
  JAVA_CMD=$(command -v java)
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

if [ ! -x "$JAVA_CMD" ]; then
  echo "Error: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
  exit 1
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
  echo "Gradle wrapper JAR not found. Please run 'gradle wrapper' or download the wrapper jar." >&2
  exit 1
fi

exec "$JAVA_CMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
