param(
    [switch]$Release = $false,
    [string]$Target = "aarch64-linux-android",
    [string]$Abi = "arm64-v8a",
    [string]$OutputDir
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$rustRoot = Join-Path $repoRoot "rust"

if (-not $OutputDir) {
    $OutputDir = Join-Path $repoRoot "android/app/build/generated/rustJniLibs/$Abi"
}

$installedTargets = rustup target list --installed
if ($installedTargets -notcontains $Target) {
    throw "Rust target '$Target' is not installed. Run: rustup target add $Target"
}

$sdkRoot = $env:ANDROID_NDK_HOME
if (-not $sdkRoot) {
    $androidHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
    if (-not $androidHome) {
        throw "ANDROID_HOME or ANDROID_SDK_ROOT is required to locate the Android NDK."
    }

    $ndkRoot = Join-Path $androidHome "ndk"
    $latestNdk = Get-ChildItem -LiteralPath $ndkRoot -Directory |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if (-not $latestNdk) {
        throw "No Android NDK found under $ndkRoot."
    }
    $sdkRoot = $latestNdk.FullName
}

$toolchain = Join-Path $sdkRoot "toolchains/llvm/prebuilt/windows-x86_64/bin"
$clang = Join-Path $toolchain "aarch64-linux-android26-clang.cmd"
$ar = Join-Path $toolchain "llvm-ar.exe"

if (-not (Test-Path $clang)) {
    throw "Android clang not found: $clang"
}
if (-not (Test-Path $ar)) {
    throw "Android llvm-ar not found: $ar"
}

$env:CC_aarch64_linux_android = $clang
$env:AR_aarch64_linux_android = $ar
$env:CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER = $clang

Push-Location $rustRoot
try {
    # cargo build -p hyp_ffi --target $Target
    cargo build -p hyp_ffi --target $Target $(if ($Release) { "--release" })
}
finally {
    Pop-Location
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$builtLibrary = Join-Path $rustRoot "target/$Target/debug/libhyp_ffi.so"
if (-not (Test-Path $builtLibrary)) {
    throw "Expected Rust shared library was not produced: $builtLibrary"
}

Copy-Item -LiteralPath $builtLibrary -Destination (Join-Path $OutputDir "libhyp_ffi.so") -Force
Write-Host "Rust native library copied to $OutputDir"
