# Software Updater #



[Click here to view the detail wiki.](http://code.google.com/p/software-updater/wiki/Overview)

## Overview ##
Software Updater is a software/library that do software updating. It contains a library that do patch creating, downloading and patching. Besides, there is a GUI interface provided that utilize the library to do software updating.

This software is divided into four parts. They are:
  1. [Builder](Overview#3.1._Builder.md) - create patches, update [catalog](PatchesCatalog.md) etc.
  1. [Launcher](Overview#3.2._Launcher.md) - do patching and launch the software
  1. [Downloader](Overview#3.3._Downloader.md) - check and download patches from the Internet
  1. [Self-Updater](Overview#3.4._Self-Updater.md) - utility used to patch the launcher

Basically self-updater is part of the launcher so there actually contain only three parts. Launcher and downloader will be distributed accompany with your software and builder will reside in your computer. Because the downloader needs to download the patches through the Internet, so you have to prepare an Internet host space to put the patches.

## Features ##

Software Updater is not simple a 'download and replace files' updater, it do much more to make:
  1. Minimize download size
  1. Ensure integrity
  1. Rollback when update failed
  1. Resumable download and update
  1. Block unauthorized access
  1. Minimize garbage files
  1. Minimize the effort to create patches

[Click here for details.](Overview#2._Features.md)

## Simple Example ##
[Click here to read the generic text version.](SimpleExample.md)

<a href='http://www.youtube.com/watch?feature=player_embedded&v=RAwjiZDN6rw' target='_blank'><img src='http://img.youtube.com/vi/RAwjiZDN6rw/0.jpg' width='425' height=344 /></a>

## Screenshots ##

### Launcher ###
![http://software-updater.googlecode.com/svn/wiki/Launcher.png](http://software-updater.googlecode.com/svn/wiki/Launcher.png)

Q&A: [How do I change the titles and icons of the launcher?](AdvancedTutorial#How_do_I_change_the_titles_and_icons_of_the_launcher?.md)

### Patch Downloader ###
![http://software-updater.googlecode.com/svn/wiki/PatchDownloader.png](http://software-updater.googlecode.com/svn/wiki/PatchDownloader.png)

Q&A: [How do I change the titles and icons of the downloader?](AdvancedTutorial#How_do_I_change_the_titles_and_icons_of_the_downloader?.md)

## Future Plans ##

### Plans to do in future ###
  1. Support different encryption & checksum method.
  1. Send back error report through web.
  1. Besides providing aes key and iv for patch in [catalog](PatchesCatalog.md), provide another way to get from the invoker (command line or Java function).
  1. Provide command line interactive for downloader and launcher.
  1. Make GUI for builder.
  1. Execute script before/after upgrading.

## Support & Discussion ##
[Support & Discussion Group](http://groups.google.com/group/software-updater)