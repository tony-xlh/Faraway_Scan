# Faraway Scan

Barcode Scanner which is designed for scanning from far away.

![](https://github.com/xulihang/Faraway_Scan/releases/download/release/faraway_scan.gif)

## Ways to Read Barcode in a Long Distance

1. Use the camera's zoom-in feature.
2. Take a photo instead of directly doing a live scan. The highest video stream resolution of mobile cameras may range from HD to 4K. Taking a photo will produce an image with a much higher resolution.
3. Use super resolution deep learning models.

## Requirements

* Android Studio 3.2 (installed on a Linux, Mac or Windows machine)
* An Android device, or an Android Emulator

## Build and Run

1. Clone the project and open it with Android Studio.
2. In the terminal, run `./gradlew fetchTFLiteLibs` to get Tensorflow Libraries.
3. Download [Dynamsoft Barcode Reader](https://www.dynamsoft.com/barcode-reader/overview/) and copy the `DynamsoftBarcodeReaderAndroid.aar` to app/libs.
4. Download [Dynamsoft Camera Enhancer](https://www.dynamsoft.com/camera-enhancer/overview/) and copy the `DynamsoftCameraEnhancer.aar` to app/libs.
5. Select `Run -> Run app.` to build and run the app.

## Links related to Dynamsoft Barcode Reader

- [![](https://img.shields.io/badge/Download-Offline%20SDK-orange)](https://www.dynamsoft.com/barcode-reader/downloads)
- [![](https://img.shields.io/badge/Get-30--day%20FREE%20Trial%20License-blue)](https://www.dynamsoft.com/customer/license/trialLicense/?product=dbr)



