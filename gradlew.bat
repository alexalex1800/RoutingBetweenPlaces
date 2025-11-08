@echo off
set DIR=%~dp0
set APP_HOME=%DIR:~0,-1%

if not defined JAVA_HOME (
    set JAVA_CMD=java
) else (
    set JAVA_CMD=%JAVA_HOME%\bin\java.exe
)

if not exist "%JAVA_CMD%" (
    echo Error: JAVA_HOME is not set and no 'java' command could be found in your PATH.
    exit /B 1
)

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
if not exist "%CLASSPATH%" (
    echo Gradle wrapper JAR not found. Please run "gradle wrapper" or download the wrapper jar.
    exit /B 1
)

"%JAVA_CMD%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
