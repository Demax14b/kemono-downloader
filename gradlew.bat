@echo off
:: Gradle startup script for Windows

set DIR=%~dp0
set APP_BASE_NAME=%~n0
set CLASSPATH=%DIR%gradle\wrapper\gradle-wrapper.jar

if not defined JAVA_HOME (
  set JAVA_EXE=java
) else (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
