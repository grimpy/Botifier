Botifier
========

Pushes Android notifications from the notification drawer to any connected bluetooth device supporting AVRCP 1.3

* Show notifcations on car radio 
* Show application as artist
* Show summery as album
* Show full notification text as title (can be splitted over multiple chunks max length is configurable)
* Play notification via TTS (text to speech)
* Use next / previous track to navigate through notifications
* Use pause / play button to remove notifcation
* Use forward button to close notification stream might help to regain focus to previous playing audio.

To test if your media device (car radio) supports AVRCP 1.3 play a mp3 song which has id3 information set and check if this information is shown on the media device.

There are two ways to provide this application with info about notifications via accesiblities or via notification access. The later one is the prefered method which is only support on Android 4.3 and up.

To enable Botifier via Notification Access (Android 4.3 only):
Goto Settings -> Security -> Notification Access -> Enable Botifier (Setting only available if Botifier is installed)

To enable Botifier via Accessiblity:
Goto Settings -> Accessiblity -> Enable Botifier

Do not enable both methods which will cause unexpected behaviour.

Source available @ https://github.com/grimpy/Botifier
