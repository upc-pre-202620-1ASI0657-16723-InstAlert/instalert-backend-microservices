param([switch]$SkipBuild,[string]$Builder='instalert-sprint3')
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
& (Join-Path $PSScriptRoot 'Initialize-DockerLocal.ps1')
Push-Location $root
try{
 & docker compose --env-file .env.docker config --quiet
 if($LASTEXITCODE -ne 0){throw 'Invalid Compose configuration'}
 if(!$SkipBuild){
  $buildArgs=@('compose','--env-file','.env.docker','build')
  if($Builder){
   & docker buildx inspect $Builder *> $null
   if($LASTEXITCODE -ne 0){& docker buildx create --name $Builder --driver docker-container --bootstrap;if($LASTEXITCODE -ne 0){throw 'Builder creation failed'}}
   $buildArgs+=@('--builder',$Builder)
  }
  foreach($service in @('ms-account','ms-incidents','ms-communities','ms-emergencies','api-gateway','frontend')){
   & docker @buildArgs $service
   if($LASTEXITCODE -ne 0){throw "Docker image build failed: $service"}
  }
 }
 & docker compose --env-file .env.docker up -d --wait --wait-timeout 300
 if($LASTEXITCODE -ne 0){throw 'Container startup or health check failed'}
 & docker compose --env-file .env.docker ps
 Write-Output 'Docker frontend: http://localhost:4201 | Gateway: http://localhost:8085/api/v1'
}finally{Pop-Location}
