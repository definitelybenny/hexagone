# Prevent laptop from charging phone during dev work
# Usage: .\battery.ps1 off  (stop charging)
#        .\battery.ps1 on   (resume charging)

param([string]$Action)

switch ($Action) {
    "off" {
        adb shell dumpsys battery set ac 0
        adb shell dumpsys battery set usb 0
        Write-Host "Charging disabled"
    }
    "on" {
        adb shell dumpsys battery reset
        Write-Host "Charging re-enabled"
    }
    default {
        Write-Host "Usage: .\battery.ps1 [on|off]"
    }
}
