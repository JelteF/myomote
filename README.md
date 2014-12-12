myomote
=======

Control VLC on your computer with your Myo. A Myo is an armband that monitors
your hand gestures and position. Play pause your video/music by making a fist
and turn the volume up and down by moving/turning your arm.

The currently supported operations:

* Toggle pause, spread your fingers
* Changing the volume, make a fist and turn your arm
* Rewind and fast forward, wave in and wave out respectively

Before these work you have to unlock your armband. This is done by doing the
thumb to pinky gesture and making a quick movement. Locking works by doing the
thumb to pinky gesture again.

The only supported versions of VLC are 2.1+, since the http interface changed
there. To set up VLC for this remote follow the guide here:
http://samicemalone.co.uk/remote-for-vlc/install.html

### External APIs used

- VLC: https://wiki.videolan.org/VLC_HTTP_requests/
- Myo: https://developer.thalmic.com/docs/api_reference/platform/the-sdk.html
- android-vlc-remote: https://github.com/samicemalone/android-vlc-remote/

### LICENCE
This project is licensed under the GPLv3 license since it uses code from
[android-vlc-remote](https://github.com/samicemalone/android-vlc-remote).
