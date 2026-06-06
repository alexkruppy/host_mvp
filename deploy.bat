@echo off
setlocal
if "%DOCKER_HOST%"=="" set "DOCKER_HOST=tcp://192.168.1.69:2375"
docker stop rest-app 2>nul
docker rm rest-app 2>nul
docker run -d --name rest-app --restart unless-stopped -p 8080:8080 rest-app
endlocal
