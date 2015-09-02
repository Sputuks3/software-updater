<font color='red'>A more detail document will be available after the end of beta release.</font>

# Overview #



## 1. Abstract ##

Software Updater is a software/library that do software updating. It contains a library that do patch creating, downloading and patching. Besides, there is a GUI interface provided that utilize the library to do software updating.

## 2. Features ##

Software Updater is not simple a 'download and replace files' updater, it do much more to make:
  1. Minimize download size
  1. Ensure integrity
  1. Rollback when update failed
  1. Resumable download and update
  1. Block unauthorized access
  1. Minimize garbage files
  1. Minimize the effort to create patches

### 2.1. Minimize download size ###
The patch downloader will determine the minimum size of patches to download to update the software to highest possible version.

### 2.2. Ensure integrity ###
#### 2.2.1. Patches download ####
After downloading the patches, the downloader will check the file size and the checksum of the patches before patching. The downloader will re-download the patch when the file size or checksum checking failed.
#### 2.2.2. Patching ####
After patching process finished, the updater will compare the existance, file size and checksum of all files of the updated version to ensure integrity. If any checking failed, the updater will prompt the user to choose to rollback or retry.

### 2.3. Rollback when update failed ###
Whenever there is any error occurred during update process, for example,
  * old version file missing
  * size/checksum of old version file or patched file not match
  * failed to do file replacement due to file locking or no permission <sup>1</sup>
  * ... other unexpected error
, user can choose to roll back to old version and continue to use the software. At the next time to check for updates, the downloader will choose to download full-pack patch only.

  1. [Self-Updater](SelfUpdater.md) will be used to deal with file locking.

### 2.4. Resumable download and update ###
The Software Updater can resume patches downloading or patching process no matter when and how the user terminate the updater.

### 2.5. Block unauthorized access ###
#### 2.5.1. Patches Catalog ####
You can use the build tool to encrypt the [catalog](PatchesCatalog.md) using pre-defined RSA private key. The public key and exponent should be pre-distributed to the [client settings file](ClientSettings.md). For detail operation, please refer to [Advanced Tutorial](AdvancedTutorial#How_do_I_do_authentication_on_catalog.xml?.md).
#### 2.5.2. Patch ####
You can use the build tool to encrypt the patch using pre-defined AES key and IV and record them into the [catalog](PatchesCatalog.md). For detail operation, please refer to [Advanced Tutorial](AdvancedTutorial#How_do_I_encrypt_the_patch?.md).

### 2.6. Minimize garbage files ###
If you use the build tool to build a patch of specific version to another specific version, the updater will be able to remove no longer needed file from the software directory.

### 2.7. Minimize the effort to create patches ###
When you create the patches, you don't have to create patches for every version. For example, let say we have now version 1.0, 1.1 and 1.2, we want to release the version 1.3. You just need to create patch for 1.2 to 1.3, you don't have to create patches for 1.0 to 1.3 or 1.1 to 1.3. The downloader will be able to download 1.0-to-1.1, 1.1-to-1.2 and 1.2-to-1.3 patches to update the software from 1.0 to 1.3. Of course, if you like, you can still create patches for 1.0 to 1.3 and 1.1 to 1.3. By doing this, you can further minimize the patches download size as a price of your time.

After a long time of release, we may have many software version, like 1.0, 1.1, 1.2 ... 1.19, 1.20. Now we want to make a release 2.0. If we keep creating patch as mentioned above, the user needs to download 21 patches to update its version 1.0 to version 2.0. It could be very large in size and take a long time to download for that 21 patches. There is a way to deal with this case. Besides create patch from specific version to specific version, we can create a "full-pack" patch. The difference between full-pack and normal patch is, full-pack patch will just do a 'extract and replace files' process. It will still check for integrity after patching but will not remove any 'no longer needed' file. Another difference between full-pack and normal patch is, instead of specifing a 'from' version, full-pack patch accept a 'from-subsequent' version, that means any software with version higher than or equal to that 'from-subsequent' version can use this full-pack patch. By the use of the full-pack patch, we can generate a normal patch from 1.20 to 2.0 and a full-pack patch 'from-subsequent' 1.0 to 2.0. Then the downloader will determine the minimum size of patches to download to update the software to highest possible version.

(The downloader is able to download multiple patches at one time and the updater can apply multiple patches at one time.)

## 3. Briefing ##

This software is divided into four parts. They are:
  1. [Builder](Overview#3.1._Builder.md) - create patches, update [catalog](PatchesCatalog.md) etc.
  1. [Launcher](Overview#3.2._Launcher.md) - do patching and launch the software
  1. [Downloader](Overview#3.3._Downloader.md) - check and download patches from the Internet
  1. [Self-Updater](Overview#3.4._Self-Updater.md) - utility used to patch the launcher

Basically self-updater is part of the launcher so there actually contain only three parts. Launcher and downloader will be distributed accompany with your software and builder will reside in your computer. Because the downloader needs to download the patches through the Internet, so you have to prepare an Internet host space to put the patches.

### 3.1. Builder ###
The builder is a command-line tool, the following are the main functions:
```
 -catalog <mode file>       e|d for 'mode', e for encrypt, d for decrypt;
                            'file' is the catalog file
 -full <folder>             create a full patch for upgrade from all
                            version (unless specified)
 -patch <old new>           create a patch for upgrade from 'old' to
                            'new'; 'old' and 'new' are the directory of
                            the two versions
```
with the following functions also:
```
 -genkey <method length>    AES|RSA for 'method'; generate cipher key with
                            specified key length in bits
 -renew <file>              renew the IV in the AES key file
 -sha256 <file>             generate SHA-256 checksum of the file
 -validate <file>           validate a XML script file
```
with the following options available:
```
 -f,--from <version>        specify the version-from
 -fs,--from-sub <version>   specify the version-from-subsequent
 -t,--to <version>          specify the version-to
 -o,--output <file>         specify output to which file
 -k,--key <file>            specify the key file to use
```
The following are some functions for testing or for fun:
```
 -do <folder patch>         apply the patch to the specified folder
 -extract <file folder>     extract the patch 'file' to the folder
 -pack <folder>             pack the folder to a patch
 -compress <file>           compress the 'file' using XZ/LZMA2
 -decompress <file>         decompress the 'file' using XZ/LZMA2
 -diff <old new>            generate a binary diff file of 'new' from
                            'old'
 -diffpatch <file patch>    patch the 'file' with the 'patch'
 -v,--version               show the version of this software
 -vb,--verbose              turn on verbose mode, output details when
                            encounter error
```

### 3.2. Launcher ###
The launcher is responsible for applying patchs and launch the software. After executing the launcher, it will check the [client settings file](ClientSettings.md) to see if there is any patch downloaded. If there is any, it will start patching the software and prompt a GUI showing progress and message to user. After the patching finished or actually there is no any new update, the launcher will launcher the software.

If you don't like the default GUI, you can implement one yourself. See [updater.gui.UpdaterWindow](http://software-updater.googlecode.com/svn/trunk/javadoc/updater/gui/UpdaterWindow.html).

If you don't want to use the GUI, you can invoke the functions in the [updater.launcher.BatchPatcher](http://software-updater.googlecode.com/svn/trunk/javadoc/updater/launcher/BatchPatcher.html) or [updater.patch](http://software-updater.googlecode.com/svn/trunk/javadoc/updater/patch/package-summary.html) to apply patches.

### 3.3. Downloader ###
The downloader is responsible for checking new updates and download suitable patches from the Internet. There is a default GUI provided showing the download progress, speed and estimated remaining time.

If you don't like the default GUI, you can implement one yourself. See [updater.gui.UpdaterWindow](http://software-updater.googlecode.com/svn/trunk/javadoc/updater/gui/UpdaterWindow.html).

If you don't want to use the GUI, you can invoke the functions in the [updater.downloader.PatcheDownloader](http://software-updater.googlecode.com/svn/trunk/javadoc/updater/downloader/PatchDownloader.html) to download patchers.

### 3.4. Self-Updater ###
This is an updater used by the launcher. The launcher will update the software by add, replace and remove files. Sometimes, we may want to update the launcher jar file. However, when we are executing the launcher to do the update, the launcher jar file may be locked by the JVM. This make updating the launcher using the launcher unworkable. This self-updater is then made to solve this problem.

[Click here to know more about Self-Updater.](SelfUpdater.md)

## 4. Usage Example ##
Before reading more deep inside, let's have a 6 minutes video step-by-step example first.

[Click here to read the first example.](SimpleExample.md)