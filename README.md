# Vehicle Summon System for Android
Android application to run on a smart phone and send user location over internet to ACTor.

## Releases
Installable application executables are available in the `/release` directory. Within this directory, there are available archives of previous releases, along with the most recent.

To install the latest release:
1. Install Android Studio - Downloadable at https://developer.android.com/studio/install
2. Connect an Android device
3. Use the `adb` command line tool (Android Debug Bridge):
```
cd {workspace}/release/release/
sudo adb install app-release.apk
```

### Connecting to LTU-Taxi ROS node
This application is preconfigured to connect to a ROS node called LTU-Taxi, which acts as a web server at the local address 192.168.99.5 and port 8642.

To enable the connection between the mobile application and the LTU-Taxi, either:
* the mobile device and the computer running LTU-Taxi must be on the same LAN network
* --OR--
* the mobile application 'OpenVPN' must be installed

#### LAN Network option

Out-of-the-box, this mobile application is able to make a POST request to the LTU-Taxi web server, if both the mobile device and the machine running LTU-Taxi are connected to the same wireless network.

#### OpenVPN option

Using the 'OpenVPN' mobile application, allows the mobile device to run on a cellular network, while the web server is connected to the internet through some other method.

'OpenVPN' can be found on the Google Play Store here:
> https://play.google.com/store/apps/details?id=net.openvpn.openvpn&hl=en_US&gl=US

Once installed, load the VPN client configuration `PleuneNET-1.0-general.ovpn` to the device. This can be done by downloading the file from the internet (i.e., through email, LTU's Github, etc.) or by using the `adb` tool with:
```
sudo adb push {workspace}/PleuneNET-1.0-general.ovpn /data/local
```
Replace `/data/local` with an appropriate location within the device storage.

Now, 'import' the `PleuneNET-1.0-general.ovpn` configuration into the OpenVPN application from file.

_NOTE: the OpenVPN application will need to run in the background for this option to work._

## Application use

To use the application, ensure that the mobile device is able to connect to the LTU-Taxi web server following the instructions from one of the options above.

Then, start the VSS application on the device. When prompted, accept the device location permission.

Once the application is running and device location permission accepted, toggle 'ON' the "Follow Location" switch. Wait for the device to find a fixed location and center the map. Then, press the "Summon Vehicle" button.

When the location is sent, there will be a dialog message displayed with the destination Vehicle IP (i.e. LTU-Taxi web server address) and the latitude and longitude of the device.