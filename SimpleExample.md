# Simple Example #



## Introduction ##
In this example, we are going integrate the Software Updater into [Sweet Home 3D](http://www.sweethome3d.com) and make a patch from version 3.2 to 3.3.

## Video ##
<a href='http://www.youtube.com/watch?feature=player_embedded&v=RAwjiZDN6rw' target='_blank'><img src='http://img.youtube.com/vi/RAwjiZDN6rw/0.jpg' width='425' height=344 /></a>

## Preparation ##
  1. Download the software updater from [here](http://code.google.com/p/software-updater/downloads).

## Integrate the updater to the software ##
  1. Open the software's root folder.
  1. Copy **Launcher.jar** and **PatchDownloader.jar** into it.
  1. Create a new folder named **update** and a file named [client.xml](ClientSettings.md).
  1. Put the following content into [client.xml](ClientSettings.md).
```
<root>
  <version>{software version}</version>
  <storage-path>update/</storage-path>
  <launch>
    <type>command</type>
    <command>{path to executable}</command>
  </launch>
  <catalog>
    <url>{HTTP URL to the catalog}</url>
  </catalog>
</root>
```
  1. Execute the **Launcher.jar** to see if it can launch your software correctly or not.

### Common Questions ###
  * [How do I launch jar instead of executable?](AdvancedTutorial#How_do_I_launch_jar_instead_of_executable?.md)
  * [How do I use it with Java executable wrapper launch4j, JSmooth etc.)?](AdvancedTutorial#How_do_I_use_it_with_Java_executable_wrapper_(launch4j,_JSmooth.md)

## Create patches ##
  1. Move the [client.xml](ClientSettings.md) out from the software folder first, see [here](AdvancedTutorial#What_kind_of_files_should_not_be_included_in_the_patch?.md) for reason.
  1. Find the build.jar and execute `java -jar build.jar -patch $oldVersionFolder $newVersionFolder --from $currentVersionNumber --to $newVersionNumber --output upgrade.patch`
  1. Get the SHA256 checksum of the file: `java -jar build.jar -sha256 upgrade.patch -o checksum.txt`
  1. Create a file named [catalog.xml](PatchesCatalog.md) and put the following content into it:
```
<patches>
  <patch id="1">
    <version>
      <from>{old version number}</from>
      <to>{new version number}</to>
    </version>
    <download>
      <url>{HTTP URL to the patch}</url>
      <checksum>{checksum of the patch from checksum.txt}</checksum>
      <length>{size of the patch (in byte)}</length>
    </download>
  </patch>
</patches>
```
  1. Upload the [catalog.xml](PatchesCatalog.md) and **upgrade.patch** to your web hosting.

### Common Questions ###
  * [How do I add more patches for other versions?](AdvancedTutorial#How_do_I_add_more_patches_for_other_versions?.md)

## Client update the software ##
  1. Execute the **PatchDownloader.jar** to download the patch.
  1. After the downloads completed, execute the **Launcher.jar** to update and launch the software.

### Common Questions ###
  * [I don't want the client to launch the downloader manually, how do I integrate it into the software?](AdvancedTutorial#I_don't_want_the_client_to_launch_the_downloader_manually,.md)