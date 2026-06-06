@echo off
echo === JAR built: target\rest-1.0.0.jar ===
echo.
echo Copy to VM and run:
echo   scp target\rest-1.0.0.jar Dockerfile deploy.sh root@192.168.1.69:~/
echo   ssh root@192.168.1.69 "cd ~/ && chmod +x deploy.sh && ./deploy.sh"
echo.
echo Or run everything on VM:
echo   ssh root@192.168.1.69 "git clone https://github.com/alexkruppy/resttest.git /tmp/resttest && cd /tmp/resttest && docker build -t rest-app . && docker run -d --name rest-app --restart unless-stopped -p 8081:8080 rest-app"
