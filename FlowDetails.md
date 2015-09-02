# Flow Details #



<font color='red'>Note the following is the flow of the GUI.</font>

## Launcher ##
  1. Check if there is any downloaded patches recorded in [client settings file](ClientSettings.md). If there isn't any, go to step 6.
  1. Start patching. Do patching and store the patched and newly created files temporaily in the **update** folder.
  1. Do replacement/movement on all files using the files stored in the **update** folder. If there is more patches to apply, go to step 2 and do patching.
  1. If there is any file patching failed due to file locking, use the [Self-Updater](Overview#3.4._Self-Updater.md) to do patching on those files.
  1. Validate all files to make sure the client has all the files that the software needs.
  1. Launch the software.

(If the update is interrupted by any means (e.g. power off), it can resume the update by reading the update progress log.)

## Downloader ##
  1. Check if there is any update for the [patches catalog](PatchesCatalog.md) available. If there isn't any, go to step 7.
  1. Download the updated [catalog](PatchesCatalog.md).
  1. Determine the minimum size of patches to download to update the software to highest possible version. If there isn't any patch to download, go to step 7.
  1. Download available patches. Determine if there is any partly downlaoded patches and do resume download.
  1. Validate the file size and checksum of the patches. If any failed, download the patch again until 5 times reached.
  1. Record the downloaded patches in the [client settings file](ClientSettings.md).
  1. Finished.