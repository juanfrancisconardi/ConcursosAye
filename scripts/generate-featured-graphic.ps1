Add-Type -AssemblyName System.Drawing

# ---------------------------------------------------------------------------
# Featured Graphic (1024 x 500) - Google Play es-419
# Objetivo: describir con claridad la funcionalidad real y marcar que NO es oficial.
# ---------------------------------------------------------------------------

$width = 1024
$height = 500
$outputPath = "D:/Trabajo/ConcursosAye/store-listing/es-419/featured_graphic.png"

$bmp = New-Object System.Drawing.Bitmap($width, $height)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic

$navy   = [System.Drawing.Color]::FromArgb(20, 35, 59)
$blue   = [System.Drawing.Color]::FromArgb(13, 110, 204)
$gray   = [System.Drawing.Color]::FromArgb(74, 85, 104)
$soft   = [System.Drawing.Color]::FromArgb(241, 246, 254)
$border = [System.Drawing.Color]::FromArgb(210, 222, 242)
$white  = [System.Drawing.Color]::White
$warn   = [System.Drawing.Color]::FromArgb(120, 72, 24)

$navyBrush  = New-Object System.Drawing.SolidBrush($navy)
$blueBrush  = New-Object System.Drawing.SolidBrush($blue)
$grayBrush  = New-Object System.Drawing.SolidBrush($gray)
$softBrush  = New-Object System.Drawing.SolidBrush($soft)
$whiteBrush = New-Object System.Drawing.SolidBrush($white)
$warnBrush  = New-Object System.Drawing.SolidBrush($warn)

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

function Get-FittingFont($graphics, [string]$text, [string]$family, [single]$maxWidth, [single]$startSize, $style) {
    $size = $startSize
    while ($size -gt 8) {
        $f = New-Object System.Drawing.Font($family, $size, $style)
        $m = $graphics.MeasureString($text, $f)
        if ($m.Width -le $maxWidth) { return $f }
        $f.Dispose()
        $size = $size - 1
    }
    return New-Object System.Drawing.Font($family, 8, $style)
}

$fam = "Segoe UI"
$bold = [System.Drawing.FontStyle]::Bold
$reg = [System.Drawing.FontStyle]::Regular

$bgRect = New-Object System.Drawing.Rectangle(0, 0, $width, $height)
$bgBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    $bgRect,
    [System.Drawing.Color]::FromArgb(232, 241, 252),
    [System.Drawing.Color]::FromArgb(251, 253, 255),
    [System.Drawing.Drawing2D.LinearGradientMode]::Vertical)
$g.FillRectangle($bgBrush, $bgRect)

# ===========================================================================
# IZQUIERDA: titulo alineado con Play Store + funcionalidad clara
# ===========================================================================
$leftX = 56
$leftMax = 520

$iconPath = New-RoundedPath $leftX 52 56 56 14
$g.FillPath($blueBrush, $iconPath)
$g.FillEllipse($whiteBrush, ($leftX + 14), 68, 9, 9)
$g.FillRectangle($whiteBrush, ($leftX + 28), 70, 20, 5)
$g.FillEllipse($whiteBrush, ($leftX + 14), 86, 9, 9)
$g.FillRectangle($whiteBrush, ($leftX + 28), 88, 20, 5)

$titleFont1 = Get-FittingFont $g "Concursos Docentes" $fam $leftMax 40 $bold
$g.DrawString("Concursos Docentes", $titleFont1, $navyBrush, $leftX, 126)
$titleFont2 = Get-FittingFont $g "Entre Rios" $fam $leftMax 40 $bold
$g.DrawString("Entre Rios", $titleFont2, $navyBrush, $leftX, 172)

$accent = New-RoundedPath $leftX 232 88 6 3
$g.FillPath($blueBrush, $accent)

$tagFont = Get-FittingFont $g "Monitor de convocatorias publicadas en el CGE" $fam $leftMax 18 $reg
$g.DrawString("Monitor de convocatorias publicadas en el CGE", $tagFont, $grayBrush, $leftX, 252)

$disclaimerFont = New-Object System.Drawing.Font($fam, 13, $bold)
$g.DrawString("App independiente - No oficial", $disclaimerFont, $warnBrush, $leftX, 284)

$featFont = New-Object System.Drawing.Font($fam, 15, $bold)
$features = @(
    "Filtra avisos por tus palabras clave",
    "Alertas de nuevas publicaciones",
    "Lee adjuntos con OCR (PDF e imagenes)"
)
$fy = 326
foreach ($feat in $features) {
    $g.FillEllipse($blueBrush, $leftX, ($fy + 5), 11, 11)
    $featFit = Get-FittingFont $g $feat $fam ($leftMax - 28) 15 $bold
    $g.DrawString($feat, $featFit, $navyBrush, ($leftX + 22), $fy)
    $fy = $fy + 40
}

# ===========================================================================
# DERECHA: mockup generico etiquetado como ejemplo
# ===========================================================================
$panelX = 620
$panelY = 48
$panelW = 340
$panelH = 404

$shadowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(28, 20, 60, 110))
$shadowPath = New-RoundedPath ($panelX + 6) ($panelY + 8) $panelW $panelH 26
$g.FillPath($shadowBrush, $shadowPath)

$panelPath = New-RoundedPath $panelX $panelY $panelW $panelH 26
$g.FillPath($whiteBrush, $panelPath)
$panelPen = New-Object System.Drawing.Pen($border, 1.5)
$g.DrawPath($panelPen, $panelPath)

$sampleFont = New-Object System.Drawing.Font($fam, 11, $bold)
$g.DrawString("VISTA DE EJEMPLO DE LA APP", $sampleFont, $grayBrush, ($panelX + 24), ($panelY + 20))

$headFont = New-Object System.Drawing.Font($fam, 16, $bold)
$g.DrawString("Concursos Relevantes", $headFont, $navyBrush, ($panelX + 24), ($panelY + 48))
$g.FillRectangle($softBrush, ($panelX + 24), ($panelY + 78), ($panelW - 48), 2)

$items = @(
    @{ cat = "Categoria"; title = "Aviso que coincide con tu keyword"; chip = "tu palabra clave" },
    @{ cat = "Categoria"; title = "Otro aviso filtrado para vos";       chip = "alerta nueva" },
    @{ cat = "Categoria"; title = "Detalle del cargo y enlace al sitio"; chip = "ver aviso" }
)

$itemX = $panelX + 22
$itemW = $panelW - 44
$itemH = 82
$iy = $panelY + 96

$catFont = New-Object System.Drawing.Font($fam, 10, $bold)
$itemTitleFont = New-Object System.Drawing.Font($fam, 13, $bold)
$chipFont = New-Object System.Drawing.Font($fam, 10, $bold)

foreach ($item in $items) {
    $cardPath = New-RoundedPath $itemX $iy $itemW $itemH 14
    $g.FillPath($softBrush, $cardPath)

    $g.DrawString($item.cat, $catFont, $blueBrush, ($itemX + 14), ($iy + 10))

    $titleFit = Get-FittingFont $g $item.title $fam ($itemW - 28) 13 $bold
    $g.DrawString($item.title, $titleFit, $navyBrush, ($itemX + 14), ($iy + 28))

    $chipText = $item.chip
    $chipSize = $g.MeasureString($chipText, $chipFont)
    $chipW = [Math]::Min($chipSize.Width + 18, $itemW - 28)
    $chipH = 22
    $chipPath = New-RoundedPath ($itemX + 14) ($iy + 52) $chipW $chipH 11
    $chipBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(220, 234, 252))
    $g.FillPath($chipBrush, $chipPath)
    $g.DrawString($chipText, $chipFont, $blueBrush, ($itemX + 22), ($iy + 55))
    $chipBrush.Dispose()

    $iy = $iy + $itemH + 10
}

$footFont = New-Object System.Drawing.Font($fam, 10, $reg)
$g.DrawString("No gestiona inscripciones ni tramites oficiales.", $footFont, $grayBrush, ($panelX + 24), ($panelY + $panelH - 34))

$bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose()
$bmp.Dispose()
Write-Output "OK: $outputPath"
