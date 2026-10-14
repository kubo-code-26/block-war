[CmdletBinding()]
param(
    [string]$KeystorePath = (Join-Path $env:USERPROFILE '.block-war-signing\upload-keystore.jks'),
    [switch]$CheckOnly
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkPath = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\keytool.exe'))) {
    $env:JAVA_HOME
} else { 'C:\Program Files\Android\Android Studio\jbr' }
$keytool = Join-Path $jdkPath 'bin\keytool.exe'
$gradle = Join-Path $projectRoot 'gradlew.bat'
if (!(Test-Path -LiteralPath $keytool)) { throw 'JDK nao encontrado. Configure JAVA_HOME com o JDK do Android Studio.' }
if (!(Test-Path -LiteralPath $gradle)) { throw 'Gradle Wrapper nao encontrado neste projeto.' }
$keystoreFile = [IO.Path]::GetFullPath($KeystorePath)
$certificateFile = Join-Path (Split-Path -Parent $keystoreFile) 'upload-certificate.pem'
if ($CheckOnly) {
    Write-Host 'Verificacao concluida: JDK e Gradle encontrados.'
    Write-Host "Arquivo privado de assinatura: $keystoreFile"
    Write-Host "Ja existe: $(Test-Path -LiteralPath $keystoreFile)"
    Write-Host 'Nenhuma chave foi criada e nenhuma senha foi solicitada.'
    return
}
function Read-PlainSecret([string]$Prompt) {
    $secret = Read-Host $Prompt -AsSecureString
    $buffer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($buffer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($buffer); $secret.Dispose() }
}
# These process-only variables are inherited by keytool/Gradle and restored in finally.
$previous = @{}
foreach ($entry in @('BW_UPLOAD_PASSWORD','KEYSTORE_PATH','STORE_PASSWORD','KEY_PASSWORD','JAVA_HOME')) {
    $previous[$entry] = [Environment]::GetEnvironmentVariable($entry, 'Process')
}
$password = $null
$confirmation = $null
Push-Location -LiteralPath $projectRoot
try {
    if (!(Test-Path -LiteralPath $keystoreFile)) {
        Write-Host 'Primeira chave de upload. Escolha e guarde uma senha propria, com pelo menos 12 caracteres.'
        $password = Read-PlainSecret 'Nova senha (nao sera mostrada)'
        $confirmation = Read-PlainSecret 'Repita a senha'
        if ($password.Length -lt 12) { throw 'Use pelo menos 12 caracteres. Nenhuma chave foi criada.' }
        if (![string]::Equals($password, $confirmation, [StringComparison]::Ordinal)) { throw 'As senhas nao conferem. Nenhuma chave foi criada.' }
        $env:BW_UPLOAD_PASSWORD = $password
        New-Item -ItemType Directory -Path (Split-Path -Parent $keystoreFile) -Force | Out-Null
        & $keytool -genkeypair -noprompt -alias upload -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Block War Upload' -storetype PKCS12 -keystore $keystoreFile -storepass:env BW_UPLOAD_PASSWORD -keypass:env BW_UPLOAD_PASSWORD
        if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel criar a chave. O arquivo existente nao sera sobrescrito.' }
    } else {
        Write-Host 'A chave ja existe. Ela sera reutilizada, sem gerar outra.'
        $password = Read-PlainSecret 'Senha da chave de upload'
        $env:BW_UPLOAD_PASSWORD = $password
    }
    & $keytool -exportcert -rfc -alias upload -keystore $keystoreFile -storepass:env BW_UPLOAD_PASSWORD -file $certificateFile
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel abrir o alias upload. Confira o arquivo e a senha.' }
    $env:KEYSTORE_PATH = $keystoreFile
    $env:STORE_PASSWORD = $password
    $env:KEY_PASSWORD = $password
    $env:JAVA_HOME = $jdkPath
    # Do not persist signing passwords in the configuration cache or a reusable daemon.
    & $gradle :app:bundleRelease --no-configuration-cache --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'O AAB nao foi gerado. A chave criada foi preservada para a proxima tentativa.' }
    $bundle = Join-Path $projectRoot 'app\build\outputs\bundle\release\app-release.aab'
    if (!(Test-Path -LiteralPath $bundle)) { throw 'Gradle terminou sem o AAB esperado.' }
    Write-Host "AAB assinado: $bundle"
    Write-Host "Certificado publico: $certificateFile"
    Write-Host "Guarde uma copia segura da chave: $keystoreFile"
    Write-Host 'Guarde a senha separadamente. Sem chave e senha nao sera possivel repetir o upload.'
    Write-Host 'Nenhum arquivo foi enviado a Play Console por este script.'
} finally {
    $password = $null
    $confirmation = $null
    foreach ($entry in $previous.Keys) { [Environment]::SetEnvironmentVariable($entry, $previous[$entry], 'Process') }
    Pop-Location
}
