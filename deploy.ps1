$VM_USER = "vboxuser"
$VM_HOST = "192.168.1.69"
$VM_DIR = "/home/$VM_USER/host_mvp"
$IMAGE_NAME = "spring_rest-app:latest"
$TAR_FILE = "host_mvp-image.tar"

Write-Host "=== Building Docker image ==="
docker compose build
if ($LASTEXITCODE -ne 0) { throw "Docker build failed" }

Write-Host "=== Saving image ==="
docker save $IMAGE_NAME -o $TAR_FILE
if ($LASTEXITCODE -ne 0) { throw "Docker save failed" }

Write-Host "=== Creating directory on VM ==="
ssh "${VM_USER}@${VM_HOST}" "mkdir -p $VM_DIR"
if ($LASTEXITCODE -ne 0) { throw "SSH mkdir failed" }

Write-Host "=== Copying files to VM ==="
scp $TAR_FILE "${VM_USER}@${VM_HOST}:$VM_DIR/"
scp docker-compose.yml "${VM_USER}@${VM_HOST}:$VM_DIR/"
if ($LASTEXITCODE -ne 0) { throw "SCP failed" }

Write-Host "=== Loading image and starting containers on VM ==="
ssh "${VM_USER}@${VM_HOST}" @"
cd $VM_DIR
docker load -i $TAR_FILE
docker compose up -d
"@
if ($LASTEXITCODE -ne 0) { throw "Deploy on VM failed" }

Write-Host "=== Cleaning up ==="
Remove-Item $TAR_FILE -Force -ErrorAction SilentlyContinue

Write-Host "=== Done! App deployed to http://$VM_HOST`:8081 ==="
