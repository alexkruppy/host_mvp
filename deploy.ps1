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

$commonPlink = @("-pw", $Password, "-t", "-hostkey", "ssh-ed25519 255 SHA256:yiTVPVTA3WVXb9RKzasGQQ4vrjBMFqTfCS7U8NmfATg")
$commonPscp = @("-pw", $Password, "-hostkey", "ssh-ed25519 255 SHA256:yiTVPVTA3WVXb9RKzasGQQ4vrjBMFqTfCS7U8NmfATg")

Write-Host "=== Add user to docker group (password needed once) ==="
& $Plink $commonPlink "${VmUser}@${VmHost}" "echo $Password | sudo -S usermod -aG docker $VmUser" 2>&1 | Out-Null

Write-Host "=== Creating remote directory ==="
& $Plink $commonPlink "${VmUser}@${VmHost}" "mkdir -p /tmp/rest-deploy"

Write-Host "=== Copying files to VM ==="
& $Pscp $commonPscp "$JarPath" "${VmUser}@${VmHost}:/tmp/rest-deploy/app.jar"
& $Pscp $commonPscp Dockerfile "${VmUser}@${VmHost}:/tmp/rest-deploy/"
& $Pscp $commonPscp deploy.sh "${VmUser}@${VmHost}:/tmp/rest-deploy/"

Write-Host "=== Building and running on VM ==="
& $Plink $commonPlink "${VmUser}@${VmHost}" "cd /tmp/rest-deploy && chmod +x deploy.sh && echo $Password | sudo -S docker build -t rest-app . && echo $Password | sudo -S docker stop rest-app 2>/dev/null; echo $Password | sudo -S docker rm rest-app 2>/dev/null; echo $Password | sudo -S docker run -d --name rest-app --restart unless-stopped -p 8081:8080 rest-app"

Write-Host "=== Done! http://${VmHost}:8081 ==="
