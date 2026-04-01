#!/bin/bash
# Prevent laptop from charging phone during dev work
# Usage: bash battery.sh off  (stop charging)
#        bash battery.sh on   (resume charging)

export MSYS_NO_PATHCONV=1

if [ "$1" = "off" ]; then
    adb shell dumpsys battery set ac 0
    adb shell dumpsys battery set usb 0
    echo "Charging disabled"
elif [ "$1" = "on" ]; then
    adb shell dumpsys battery reset
    echo "Charging re-enabled"
else
    echo "Usage: bash battery.sh [on|off]"
fi
