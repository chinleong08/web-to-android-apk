param(
    [Parameter(Mandatory)][uri]$Url,
    [Parameter(Mandatory)][ValidateNotNullOrEmpty()][string]$AppName,
    [Parameter(Mandatory)][ValidatePattern('^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$')][string]$PackageId,
    [Parameter(Mandatory)][string]$Icon,
    [Parameter(Mandatory)][string]$Jdk,
    [Parameter(Mandatory)][string]$BuildTools,
    [Parameter(Mandatory)][string]$PlatformJar,
    [Parameter(Mandatory)][string]$SigningKey,
    [Parameter(Mandatory)][string]$KeyAlias,
    [Parameter(Mandatory)][string]$BuildDirectory,
    [Parameter(Mandatory)][string]$Output,
    [ValidateRange(1, 2100000000)][int]$VersionCode = 1,
    [ValidatePattern('^[0-9][0-9A-Za-z.+_-]*$')][string]$VersionName = '1.0.0'
)

$ErrorActionPreference = 'Stop'
if (!$Url.IsAbsoluteUri -or $Url.Scheme -ne 'https' -or $Url.UserInfo) {
    throw 'Supply an absolute HTTPS URL without embedded credentials.'
}
if (!$env:APK_SIGNING_PASSWORD) { throw 'Set APK_SIGNING_PASSWORD for the signing key.' }
if ([IO.Path]::GetExtension($Output) -ne '.apk' -or (Test-Path -LiteralPath $Output)) {
    throw 'Choose a new output path ending in .apk.'
}
if ((Test-Path -LiteralPath $BuildDirectory)) { throw 'Choose a new build directory.' }
$SigningKey = (Resolve-Path -LiteralPath $SigningKey).Path
$Icon = (Resolve-Path -LiteralPath $Icon).Path
$apkPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Output)

function Invoke-BuildTool($Executable, [string[]]$Arguments) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Build step failed: $Executable" }
}

$templates = Join-Path $PSScriptRoot '../assets'
$build = New-Item -ItemType Directory -Path $BuildDirectory
$classes = New-Item -ItemType Directory -Path (Join-Path $build.FullName 'classes')
$dex = New-Item -ItemType Directory -Path (Join-Path $build.FullName 'dex')
$resources = New-Item -ItemType Directory -Path (Join-Path $build.FullName 'res/mipmap-nodpi') -Force
Copy-Item -LiteralPath $Icon -Destination (Join-Path $resources.FullName 'ic_launcher.png')

$sourcePath = Join-Path $build.FullName 'MainActivity.java'
$manifestPath = Join-Path $build.FullName 'AndroidManifest.xml'
$source = Get-Content -LiteralPath (Join-Path $templates 'MainActivity.java') -Raw
$urlLiteral = $Url.AbsoluteUri.Replace('\', '\\').Replace('"', '\"')
$source.Replace('__PACKAGE__', $PackageId).Replace('__URL__', $urlLiteral) |
    Set-Content -LiteralPath $sourcePath -Encoding utf8
$manifest = Get-Content -LiteralPath (Join-Path $templates 'AndroidManifest.xml') -Raw
$label = [Security.SecurityElement]::Escape($AppName)
if ($label.StartsWith('@') -or $label.StartsWith('?')) { $label = '\' + $label }
$manifest.Replace('__PACKAGE__', $PackageId).Replace('__VERSION_CODE__', [string]$VersionCode).
    Replace('__VERSION_NAME__', $VersionName).Replace('__APP_NAME__', $label) |
    Set-Content -LiteralPath $manifestPath -Encoding utf8

$java = Join-Path $Jdk 'bin/java.exe'
$javac = Join-Path $Jdk 'bin/javac.exe'
$aapt = Join-Path $BuildTools 'aapt2.exe'
$compiled = Join-Path $build.FullName 'resources.zip'
$unsigned = Join-Path $build.FullName 'unsigned.apk'
$aligned = Join-Path $build.FullName 'aligned.apk'

Invoke-BuildTool $aapt @('compile', '--dir', (Join-Path $build.FullName 'res'), '-o', $compiled)
Invoke-BuildTool $aapt @('link', '-I', $PlatformJar, '--manifest', $manifestPath, '-o', $unsigned, $compiled)
Invoke-BuildTool $javac @('--release', '8', '-classpath', $PlatformJar, '-d', $classes.FullName, $sourcePath)
$classFiles = (Get-ChildItem -LiteralPath $classes.FullName -Recurse -Filter '*.class').FullName
Invoke-BuildTool $java (@('-cp', (Join-Path $BuildTools 'lib/d8.jar'), 'com.android.tools.r8.D8', '--release',
    '--min-api', '26', '--lib', $PlatformJar, '--output', $dex.FullName) + $classFiles)

Add-Type -AssemblyName System.IO.Compression.FileSystem
$apk = [IO.Compression.ZipFile]::Open($unsigned, 'Update')
try {
    [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($apk, (Join-Path $dex.FullName 'classes.dex'),
        'classes.dex', [IO.Compression.CompressionLevel]::Optimal) | Out-Null
} finally {
    $apk.Dispose()
}

$zipalign = Join-Path $BuildTools 'zipalign.exe'
$signer = Join-Path $BuildTools 'lib/apksigner.jar'
$signed = Join-Path $build.FullName 'signed.apk'
Invoke-BuildTool $zipalign @('-f', '4', $unsigned, $aligned)
Invoke-BuildTool $java @('-jar', $signer, 'sign', '--ks', $SigningKey, '--ks-key-alias', $KeyAlias,
    '--ks-pass', 'env:APK_SIGNING_PASSWORD', '--v4-signing-enabled', 'false', '--out', $signed, $aligned)
Invoke-BuildTool $java @('-jar', $signer, 'verify', '--verbose', '--print-certs', $signed)
Invoke-BuildTool $zipalign @('-c', '4', $signed)
Invoke-BuildTool $aapt @('dump', 'badging', $signed)
Copy-Item -LiteralPath $signed -Destination $apkPath
Get-FileHash -LiteralPath $apkPath -Algorithm SHA256
