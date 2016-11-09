Migrator for Android OS: migrate Android applications
===

Migrator is the platform for Android 4.4(KitKat) to migrate your applications between devices.
You can change working devices easily and seamlessly.

## Description
Install this platform to all devices you have, then, you can migrate applications you use now.
For example, you write a document with a small device like a smartphone, in the next moment, you write a same document with a bigger one like a tablet.
This platform sends files and statuses of applications to another device and restart your application in another device.

## Demo
See [Demo](https://www.ssn.nitech.ac.jp/研究内容/migrator/#demo)

## Requirement
At least two devices with Bluetooth.

## Install
To install this platform, you must build Android 4.4.
Access [AOSP](https://source.android.com/source/requirements.html) to get information to build.
After downloading sources, replace the original files to our files.
Run `setup.sh` to replace.
```sh
$ ./setup.sh $ANDROID_HOME
```

## Usage
You can see a notification icon in the status bar.
Tapping this icon, you can migrate your application to another device.
In addition, `migrate()` and `migrate(String[])` methods are available.
These methods is defined in `Activity.java`, so developers can use these methods in all Activities.
If they are called, the platform executes migration immediately.

## Document
WANC: Kohei Sato, Koichi Mouri and Shoichi Saito: "Design and implementation of an application state migration mechanism between Android devices",  7th International Workshop on Advances in Networking and Computing (WANC), to appear (2016.11).
