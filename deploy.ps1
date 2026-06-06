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
    Invoke-WebRequest -Uri "https://the.earth.li/~sgtatham/putty/latest-w64/plink.exe" -OutFile $Plink -UseBasicParsing
}
if (!(Test-Path $Pscp)) {
    Write-Host "Downloading pscp.exe..."
    Invoke-WebRequest -Uri "https://the.earth.li/~sgtatham/putty/latest-w64/pscp.exe" -OutFile $Pscp -UseBasicParsing
}

$common = "-pw $Password -batch"

Write-Host "=== Creating remote directory ==="
& $Plink $common "${VmUser}@${VmHost}" "mkdir -p ~/rest-deploy"

Write-Host "=== Copying files to VM ==="
& $Pscp $common "$JarPath" "${VmUser}@${VmHost}:~/rest-deploy/app.jar"
& $Pscp $common Dockerfile "${VmUser}@${VmHost}:~/rest-deploy/"
& $Pscp $common deploy.sh "${VmUser}@${VmHost}:~/rest-deploy/"

Write-Host "=== Building and running on VM ==="
& $Plink $common "${VmUser}@${VmHost}" "cd ~/rest-deploy && chmod +x deploy.sh && sudo ./deploy.sh"

Write-Host "=== Done! http://${VmHost}:8081 ==="
