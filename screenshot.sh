#!/bin/bash
export MSYS_NO_PATHCONV=1
adb shell "screencap /sdcard/screenshot.png"
adb pull /sdcard/screenshot.png C:/dev/hex-flipper/screenshot.png
