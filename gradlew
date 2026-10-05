#!/bin/sh
set -e
if [ ! -f "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" ]; then
  echo "Gradle wrapper JAR is not bundled in this generated environment."
  echo "Open the project in Android Studio or run with a locally installed Gradle 8.10.2+."
  exit 1
fi
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
