param(
    [string]$VmUser = "vboxuser",
    [string]$VmHost = "192.168.1.69",
    [string]$Password = "bloodmaniac",
    [string]$JarPath = "target/rest-1.0.0.jar"
)

$ErrorActionPreference = "Stop"
$Plink = "$PSScriptRoot\plink.exe"
$Pscp = "$PSScriptRoot\pscp.exe"

# Скачать plink + pscp если нет
if (!(Test-Path $Plink)) {
    Write-Host "Downloading plink.exe..."
    Invoke-WebRequest -Uri "https://the.earth.li/~sgtatham/putty/latest/w64/plink.exe" -OutFile $Plink -UseBasicParsing
}
if (!(Test-Path $Pscp)) {
    Write-Host "Downloading pscp.exe..."
    Invoke-WebRequest -Uri "https://the.earth.li/~sgtatham/putty/latest/w64/pscp.exe" -OutFile $Pscp -UseBasicParsing
}

$common = @("-pw", $Password, "-t", "-hostkey", "ssh-ed25519 255 SHA256:yiTVPVTA3WVXb9RKzasGQQ4vrjBMFqTfCS7U8NmfATg")

Write-Host "=== Adding user to docker group ==="
& $Plink $common "${VmUser}@${VmHost}" "echo $Password | sudo -S usermod -aG docker $VmUser" 2>&1 | Out-Null

Write-Host "=== Creating remote directory ==="
& $Plink $common "${VmUser}@${VmHost}" "mkdir -p /tmp/rest-deploy"

Write-Host "=== Copying files to VM ==="
& $Pscp $common "$JarPath" "${VmUser}@${VmHost}:/tmp/rest-deploy/app.jar"
& $Pscp $common Dockerfile "${VmUser}@${VmHost}:/tmp/rest-deploy/"
& $Pscp $common deploy.sh "${VmUser}@${VmHost}:/tmp/rest-deploy/"

Write-Host "=== Reconnecting to apply docker group ==="
& $Plink $common "${VmUser}@${VmHost}" "newgrp docker && cd /tmp/rest-deploy && chmod +x deploy.sh && ./deploy.sh"

Write-Host "=== Done! http://${VmHost}:8081 ==="
