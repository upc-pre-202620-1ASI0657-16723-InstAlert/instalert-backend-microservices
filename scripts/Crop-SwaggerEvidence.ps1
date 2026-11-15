param([string]$SourceDirectory)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$manifest=Get-Content -LiteralPath (Join-Path $SourceDirectory 'manifest.json') -Raw | ConvertFrom-Json
foreach($record in $manifest){
 $source=Join-Path $SourceDirectory ($record.filename+'-full.jpg')
 $image=[Drawing.Bitmap]::new($source)
 try{
  $bands=@();$start=-1;$center=[int]($image.Width/2)
  for($y=0;$y -lt $image.Height;$y++){
   $pixel=$image.GetPixel($center,$y);$match=$pixel.R -gt 190 -and $pixel.B -gt 170 -and $pixel.G -lt 100
   if($match -and $start -lt 0){$start=$y}
   if(!$match -and $start -ge 0){if(($y-$start) -ge 3){$bands+=,@($start,$y)};$start=-1}
  }
  if($bands.Count -eq 4 -and ($bands[1][0]-$bands[0][1]) -lt 25){$bands=@(@($bands[0][0],$bands[1][1]),$bands[2],$bands[3])}
  if($bands.Count -ne 3){throw "Expected three crop boundaries for $($record.filename); found $($bands.Count)"}
  $row=[int](($bands[0][0]+$bands[0][1])/2);$left=0;$right=$image.Width-1
  for($x=0;$x -lt $image.Width;$x++){$p=$image.GetPixel($x,$row);if($p.R -gt 190 -and $p.B -gt 170 -and $p.G -lt 100){$left=$x;break}}
  for($x=$image.Width-1;$x -gt $left;$x--){$p=$image.GetPixel($x,$row);if($p.R -gt 190 -and $p.B -gt 170 -and $p.G -lt 100){$right=$x;break}}
  foreach($part in 0,1){
   $top=$bands[$part][1]+1;$bottom=$bands[$part+1][0]-1
   $rectangle=[Drawing.Rectangle]::new($left,$top,$right-$left+1,$bottom-$top)
   $crop=$image.Clone($rectangle,$image.PixelFormat)
   try{$suffix=if($part -eq 0){'.PNG'}else{'-2.PNG'};$crop.Save((Join-Path $SourceDirectory ($record.filename+$suffix)),[Drawing.Imaging.ImageFormat]::Png)}finally{$crop.Dispose()}
  }
  Write-Output "$($record.filename): request + response cropped"
 }finally{$image.Dispose()}
}
