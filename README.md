# line-msg-bridge (LINE Msg Bridge for Android Auto)

This app bridges messages of LINE application to Android Auto. Although LINE itself does not support Android Auto,  you can view the messages of LINE and reply to it on Android Auto by using this app.

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
- [Limitations](#limitations)
- [License](#license)

## Installation

Build using Android Studio and generate apk, and install it manually. Currently this app is not on Google Play Store.

## Usage

+ This app needs permission to read notification. After installed, click "Permission to read notification" switch and grant permission to this app. It may be required to set `Allow restricted settings` before granting the permission.

+ After Android 13, notification post permission is required. Grant them on first startup of application.

## Limitations

+ This app assumes of the behavior of "current" LINE app. Change of LINE app behavior may make this app not work correctly. 

+ "Mark as read" function of Android Auto is not supported.

## License

MIT License