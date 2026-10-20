@echo off
cd /d "%~dp0"
echo Campus Forum - http://127.0.0.1:8081
if exist "forum-service\target\forum-service-1.0.0.jar" (
  java -jar "forum-service\target\forum-service-1.0.0.jar"
) else (
  call mvn -pl forum-service spring-boot:run
)
pause
