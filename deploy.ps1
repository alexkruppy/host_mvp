param(
    [string]$VmUser = "vboxuser",
    [string]$VmHost = "192.168.1.69",
    [string]$JarPath = "target/rest-1.0.0.jar"
)

$ErrorActionPreference = "Stop"

Write-Host "=== Copying JAR + Dockerfile to VM ==="
scp "$JarPath" "${VmUser}@${VmHost}:~/app.jar"
scp Dockerfile "${VmUser}@${VmHost}:~/Dockerfile"
scp deploy.sh "${VmUser}@${VmHost}:~/deploy.sh"

Write-Host "=== Building and deploying on VM ==="
ssh "${VmUser}@${VmHost}" "cd ~/ && chmod +x deploy.sh && sudo ./deploy.sh"

Write-Host "=== Done! http://${VmHost}:8081 ==="
