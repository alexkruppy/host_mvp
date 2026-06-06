@echo off
setlocal
set VM=root@192.168.1.69
set JAR=target\rest-1.0.0.jar
set REMOTE_DIR=/tmp/rest-deploy

echo Copying JAR to VM...
scp "%JAR%" %VM%:%REMOTE_DIR%/app.jar
scp Dockerfile %VM%:%REMOTE_DIR%/

echo Building Docker image on VM...
ssh %VM% "cd %REMOTE_DIR% && docker build -t rest-app ."

echo Stopping old container...
ssh %VM% "docker stop rest-app 2>/dev/null; docker rm rest-app 2>/dev/null"

echo Starting new container...
ssh %VM% "docker run -d --name rest-app --restart unless-stopped -p 8080:8080 rest-app"

echo Done!
endlocal
