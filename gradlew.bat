@ECHO OFF
SETLOCAL

SET APP_BASE_NAME=%~n0
SET APP_HOME=%~dp0

SET DEFAULT_JVM_OPTS=-Xmx64m -Xms64m

SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

IF NOT EXIST "%CLASSPATH%" (
  ECHO gradle-wrapper.jar not found. Please run "gradle wrapper" with a local Gradle installation.>&2
  EXIT /B 1
)

"%JAVA_HOME%\bin\java.exe" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
