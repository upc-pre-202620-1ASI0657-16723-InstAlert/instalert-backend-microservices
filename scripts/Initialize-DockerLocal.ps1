$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$file=Join-Path $root '.env.docker'
$lines=@(); if(Test-Path -LiteralPath $file){$lines=@([IO.File]::ReadAllLines($file))}
$names=@('ACCOUNT_DB_PASSWORD','ACCOUNT_DB_ROOT_PASSWORD','EMERGENCIES_DB_PASSWORD','EMERGENCIES_DB_ROOT_PASSWORD','MONOLITH_DB_PASSWORD','MONOLITH_DB_ROOT_PASSWORD','INCIDENTS_DB_PASSWORD','INCIDENTS_DB_ROOT_PASSWORD','COMMUNITIES_DB_PASSWORD','COMMUNITIES_DB_ROOT_PASSWORD','ACCOUNT_JWT_SECRET','NOTIFICATIONS_INTERNAL_KEY')
$rng=[Security.Cryptography.RandomNumberGenerator]::Create()
try{foreach($name in $names){if(!($lines -match "^$name=")){$bytes=New-Object byte[] 32;$rng.GetBytes($bytes);$value=[BitConverter]::ToString($bytes).Replace('-','');$lines+="$name=$value"}}}finally{$rng.Dispose()}
foreach($pair in @('FRONTEND_DOCKER_PORT=4201','GATEWAY_DOCKER_PORT=8085')){if(!($lines -match ('^'+$pair.Split('=')[0]+'='))){$lines+=$pair}}
[IO.File]::WriteAllLines($file,$lines)
Write-Output 'Private Docker configuration initialized; existing credentials preserved.'
