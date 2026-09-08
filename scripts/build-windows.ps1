param(
    [string]$JdkHome = 'C:\Program Files\Java\jdk-17\jdk-17.0.18+8',
    [string[]]$Tasks = @(':app:assembleDebug', ':app:testDebugUnitTest'),
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$javaExecutable = Join-Path $JdkHome 'bin\java.exe'
if (!(Test-Path -LiteralPath $javaExecutable)) {
    throw '找不到 JDK 17，请通过 -JdkHome 指定已安装的 JDK 17 目录。'
}

# Windows Java 的本地管道需要较短的临时路径；只对本次构建进程生效。
$pipeDirectory = Join-Path $env:USERPROFILE '.gradle\ux-tmp'
New-Item -ItemType Directory -Path $pipeDirectory -Force | Out-Null
$previousJavaHome = $env:JAVA_HOME
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_HOME = $JdkHome
    $env:JAVA_TOOL_OPTIONS = "$previousJavaOptions `"-Djdk.net.unixdomain.tmpdir=$pipeDirectory`"".Trim()
    Push-Location -LiteralPath $projectDirectory
    try {
        $gradleArguments = @('--console=plain') + $Tasks
        if ($Offline) { $gradleArguments += '--offline' }
        & $javaExecutable -classpath (Join-Path $projectDirectory 'gradle\wrapper\gradle-wrapper.jar') org.gradle.wrapper.GradleWrapperMain @gradleArguments
        if ($LASTEXITCODE -ne 0) { throw "Gradle 构建失败，退出码：$LASTEXITCODE" }
    } finally {
        Pop-Location
    }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:JAVA_TOOL_OPTIONS = $previousJavaOptions
}
