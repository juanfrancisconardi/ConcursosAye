Add-Type -AssemblyName System.Drawing

# Icono 512x512 alineado con la app (lista/convocatorias). Reemplaza el mate por algo relevante.
$size = 512
$root = "D:/Trabajo/ConcursosAye"
$outputs = @(
    "$root/app/src/main/res/drawable-nodpi/ic_launcher_fg.png"
)

$blue   = [System.Drawing.Color]::FromArgb(13, 110, 204)
$blueDk = [System.Drawing.Color]::FromArgb(20, 35, 59)
$white  = [System.Drawing.Color]::White

function New-RoundedPath([single]$x, [single]$y, [single]$w, [single]$h, [single]$r) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

foreach ($outputPath in $outputs) {
    $dir = Split-Path $outputPath -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }

    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear($blue)

    $pad = [single]($size * 0.14)
    $card = New-RoundedPath $pad $pad ($size - 2 * $pad) ($size - 2 * $pad) ($size * 0.12)
    $g.FillPath((New-Object System.Drawing.SolidBrush($white)), $card)

    $innerX = $pad + ($size * 0.12)
    $innerY = $pad + ($size * 0.14)
    $lineW = $size * 0.42
    $lineH = $size * 0.05
    $dot = $size * 0.06
    $gap = $size * 0.11

    $blueBrush = New-Object System.Drawing.SolidBrush($blue)
    $dkBrush = New-Object System.Drawing.SolidBrush($blueDk)

    for ($i = 0; $i -lt 3; $i++) {
        $y = $innerY + ($i * $gap)
        $g.FillEllipse($blueBrush, $innerX, $y, $dot, $dot)
        $g.FillRectangle($dkBrush, ($innerX + $dot + 10), ($y + $dot * 0.18), $lineW, $lineH)
    }

    $g.Dispose()
    $bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "OK: $outputPath"
}
