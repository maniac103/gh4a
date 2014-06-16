OctoDroid
=========
This application provides access to [GitHub](https://github.com/) and lets you stay connected with your network

![Octodroid](https://raw.githubusercontent.com/slapperwan/gh4a/master/res/drawable-xxhdpi/octodroid.png)

[![Build Status](https://drone.io/github.com/slapperwan/gh4a/status.png)](https://drone.io/github.com/slapperwan/gh4a/latest)

Download
--------
[![Download OctoDroid from Google Play](http://www.android.com/images/brand/android_app_on_play_large.png)](https://play.google.com/store/apps/details?id=com.gh4a) [![Download OctoDroid from F-Droid.org](https://raw.githubusercontent.com/kageiit/images-host/master/badges/fdroid-badge.png)](http://f-droid.org/repository/browse/?fdfilter=octodroid&fdid=com.gh4a) [![Download latest debug from drone.io](https://raw.githubusercontent.com/kageiit/images-host/master/badges/drone-io-badge.png)](https://drone.io/github.com/slapperwan/gh4a/files/build/apk/gh4a-debug.apk)

Main features
-------------

###Repository###
* List repositories
* Watch/unwatch repository
* View branches/tags
* View pull requests
* View contributors
* View watchers/networks
* View issues

###User###
* View basic information
* Activity feeds
* Follow/unfollow user
* View public/watched repositories
* View followers/following
* View organizations (if type is user)
* View members (if type is organization)

###Issue###
* List issues
* Filter by label, assignee or milestone
* Create/edit/close/reopen issue
* Comment on issue
* Manage labels
* Manage milestones

###Commit###
* View commit (shows files changed/added/deleted)
* Diff viewer with colorized HTML
* View commit history on each file

###Tree/File browser###
* Browse source code
* View code with syntax hightlighting

###Gist###
* List public gists
* View gist content

###Explore Github###
* Public timeline
* Trending repos (today, week, month, forever)
* GitHub blog

*..and many more*

How to Build Octodroid
----------------------
- Ensure Android SDK platform version 19 and build-tools version 19.1.0 are installed
- Build using Gradle

```bash
./gradlew assembleDebug
```

- To get a full list of available tasks

```bash
./gradlew tasks
```

Open Source Libraries
---------------------
* [GitHub Java API](https://github.com/maniac103/egit-github/tree/master/org.eclipse.egit.github.core)
* [ActionBarSherlock](https://github.com/JakeWharton/ActionBarSherlock) tag 4.4.0
* [ViewPagerIndicator](https://github.com/JakeWharton/Android-ViewPagerIndicator)
* [HoloColorPicker](https://github.com/LarsWerkman/HoloColorPicker)
* [ProgressFragment](https://github.com/johnkil/Android-ProgressFragment)

Contributions
-------------
* [kageiit](https://github.com/kageiit) - Improvements and bug fixes
* [maniac103](https://github.com/maniac103) - Improvements, bug fixes and new features
* [ARoiD](https://github.com/ARoiD) - Testing
* [extremis (Steven Mautone)](https://github.com/extremis) - OctoDroid name and the new icon
* [zquestz](https://github.com/zquestz) - Thanks for the application icon
* [cketti](https://github.com/cketti)
