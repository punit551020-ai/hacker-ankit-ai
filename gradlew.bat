@ECHO OFF
IF NOT EXIST "%~dp0gradle\wrapper\gradle-wrapper.jar" (
  ECHO Gradle wrapper JAR is not bundled in this generated environment.
  ECHO Open the project in Android Studio or install Gradle 8.10.2+ locally.
  EXIT /B 1
)
java -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
