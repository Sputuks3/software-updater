# Advanced Tutorial #



## Launcher ##

### How do I launch jar instead of executable? ###
Update the [client.xml](ClientSettings.md), change the launch type to **jar**, specify the path to the jar and the main class to launch, e.g.:
```
<root>
  ...
  <launch>
    <type>jar</type>
    <jar-path>LanguageFilesTool.jar</jar-path>
    <main-class>langfiles.Main</main-class>
  </launch>
  ...
</root>
```
By using this method, the launcher will load the jar by class loader and invoke the main method of specified main-class.

Besides this, you can also launch the jar by command, e.g.:
```
<root>
  ...
  <launch>
    <type>command</type>
    <command>{java} -jar LanguageFilesTool.jar</command>
  </launch>
  ...
</root>
```
You can use **{java}** instead the path to the Java binary, we will replace **{java}** by the path of the Java binary for you.

### How do I use it with Java executable wrapper (launch4j, JSmooth etc.)? ###
No matter you use **wrap** (include the jar in the exe) or **launch only** (jar store separately with exe), you can change your jar to the launcher. We will load your jar instead of launching your jar in separate process (if launch-type is **jar**).

### What if the command parameter contains space? ###
We accept multiple commands in sequence, we will pass those commands to [ProcessBuilder](http://download.oracle.com/javase/6/docs/api/java/lang/ProcessBuilder.html) to cater the quoting on different OS, e.g.:
```
<root>
  ...
  <launch>
    <type>command</type>
    <command>{java}</command>
    <command>-jar</command>
    <command>C:\My Software\main.jar</command>
  </launch>
  ...
</root>
```

### How do I change the titles and icons of the launcher? ###
Edit the [client.xml](ClientSettings.md), add the following:
```
<root>
  ...
  <information>
    ...
    <launcher>
      <name>Software Updater</name>
      <icon>
        <location>folder</location>
        <path>updater_icon.png</path>
      </icon>
    </launcher>
  </information>
  ...
</root>
```
The icon location can be either **folder** or **jar**. If it is **folder**, you have to place the image in the folder; if it is **jar**, you have to place the image inside the jar and give the resource path, e.g.:
```
...
      <icon>
        <location>jar</location>
        <path>/updater/img/updater_icon.png</path>
      </icon>
...
```

See also [here](#How_do_I_change_the_title_and_icon_of_the_window_of_launcher_and.md).

## Downloader ##

### How do I change the titles and icons of the downloader? ###
Edit the [client.xml](ClientSettings.md), add the following:
```
<root>
  ...
  <information>
    ...
    <downloader>
      <name>Patches Downloader</name>
      <icon>
        <location>folder</location>
        <path>updater_icon.png</path>
      </icon>
    </downloader>
  </information>
  ...
</root>
```
The icon location can be either **folder** or **jar**. If it is **folder**, you have to place the image in the folder; if it is **jar**, you have to place the image inside the jar and give the resource path, e.g.:
```
...
      <icon>
        <location>jar</location>
        <path>/updater/img/updater_icon.png</path>
      </icon>
...
```

See also [here](#How_do_I_change_the_title_and_icon_of_the_window_of_launcher_and.md).

### I don't want the client to launch the downloader manually, how do I integrate it into the software? ###
You can, in your software, launch the downloader using command prompt (terminal); or you can include the downloader in your class path, then invoke the functions, see [updater.downloader](http://software-updater.googlecode.com/svn/trunk/javadoc/updater/downloader/package-summary.html).

## Builder ##

### How do I add more patches for other versions? ###
In the [catalog.xml](PatchesCatalog.md), every patches has its own **`<patch>`** tag, each with different **id**. For example, we have a patch to upgrade from **1.0** to **1.1** and a patch to upgrade from **1.1** to **1.2**:
```
<patches>
  <patch id="1">
    <version>
      <from>1.0</from>
      <to>1.1</to>
    </version>
    <download>
      <url>http://localhost/1.0_1.1.patch</url>
      <checksum>cb6eae9222be257ea73c0a9ad1548ac56fcc9c2bf07b5616efe689c075f5b7ae</checksum>
      <length>48221152</length>
    </download>
  </patch>
  <patch id="2">
    <version>
      <from>1.1</from>
      <to>1.2</to>
    </version>
    <download>
      <url>http://localhost/1.1_1.2.patch</url>
      <checksum>78e88c8e98dcec995247475449a45dee1bda3a5313c8c07d7c62d804acb8d1fa</checksum>
      <length>4620314</length>
    </download>
  </patch>
</patches>
```
Normally you do not need to create the patch from **1.0** to **1.2**, the downloader can determine **the shortest path** (in download size) and download **1.0`_`1.1.patch** and **1.1`_`1.2.patch** to upgrade the software from 1.0 to 1.2.

Besides creating from version to version, we support to create a full pack of specific version to allow any version to upgrade to it. For example, now we have version 2.0, we create a full pack for it:
```
java -jar build.jar -full $softwareFolder --output --from-subsequent 1.0 --to 2.0 --output +1.0_2.0.patch
```
then edit the [catalog.xml](PatchesCatalog.md) and add
```
<patches>
  ...
  <patch id="3">
    <type>full</type>
    <version>
      <from-subsequent>1.0</from-subsequent>
      <to>2.0</to>
    </version>
    <download>
      <url>http://localhost/+1.0_2.0.patch</url>
      <checksum>21a57f2fe765e1ae4a8bf15d73fc1bf2a533f547f2343d12a499d9c0592044d4</checksum>
      <length>32578433</length>
    </download>
  </patch>
  ...
</patches>
```
Then, those software with version higher than or equal to 1.0 will consider this patch (if there is a patch available from 1.1 to 2.0, the downloader will compare the download size to determine which patch(es) to download.)

p.s. Besides **--from-subsequent**, you can also use back the **--from**.

### How do I encrypt the patch? ###
Currently we only accept AES-256. To do this, first we need to create an AES key first:
```
java -jar build.jar -genkey AES 256 --output AES.xml
```
When creating patch, you have to add the following at the end of the command:
```
--key AES.xml
```
In [catalog.xml](PatchesCatalog.md), you have to include the cipher key and initial vector (IV) for that patch:
```
<patches>
  ...
  <patch id="{patch id}">
    ...
    <download>
      ...
      <encryption>
        <type>AES</type>
        <key>{copy the key from AES.xml}</key>
        <IV>{copy the IV from AES.xml}</IV>
      </encryption>
    </download>
  </patch>
</patches>
```

After use, you have to renew the initial vector (IV) of the **AES.xml** by:
```
java -jar build.jar -renew AES.xml
```

## Other ##

### What kind of files should not be included in the patch? ###
Because we will calculate and compare the checksum before patching the file, so those files with content possibly changed in runtime, like database, log file, must not be included in the patch.

### Any limitation for the version number? ###
Yes, there is, currently accept only [0-9]+(\.[0-9]+)`*`, that is numbers separated by dot(**.**). We use it to determine which version is newer.

### How to do a quick patch testing? ###
You can apply the patch to specific folder to test whether the patch works well or not. Use **build.jar** and type:
```
java -jar build.jar -do $folderToApplyThePatch $pathToPatch
```
If the patch has been encrypted, you can specify the key to use by **--key**.

### How do I change the title and icon of the window of launcher and downloader? ###
Edit the [client.xml](ClientSettings.md), add the following:
```
<root>
  ...
  <information>
    ...
    <software>
      <name>Language Files Tool</name>
      <icon>
        <location>folder</location>
        <path>software_icon.png</path>
      </icon>
    </software>
  </information>
  ...
</root>
```
The icon location can be either **folder** or **jar**. If it is **folder**, you have to place the image in the folder; if it is **jar**, you have to place the image inside the jar and give the resource path, e.g.:
```
...
      <icon>
        <location>jar</location>
        <path>/updater/img/software_icon.png</path>
      </icon>
...
```

### How do I do authentication on catalog.xml? ###
Currently we only accept RSA. To do this, first we need to create an RSA key first:
```
java -jar build.jar -genkey RSA 512 --output RSA.xml
```
Then we encrypt the [catalog](PatchesCatalog.md) by:
```
java -jar build.jar -catalog e catalog.xml --key RSA.xml --output catalog_encrypted.xml
```
In [client.xml](ClientSettings.md), you have to include the modulus and public exponent:
```
<root>
  ...
  <catalog>
    ...
    <public-key>
      <modulus>0080ac742891f8ba0d59dcc96b464e2245e53a9b29f8219aa0b683ad10007247ced6d74b7bef2a6b0555ec22735827b2b9dfe94664d492a723ad78d6d97d1c9b19ade1225edc060eaced684436ce221659c7e8320bc2bf5ddcdbe6751b0f476066437ccc50ea0e5afafb6a59581df509145d34aa4d0541f500f09868686f5681a509bf58feda73b35326f816b60205550783d628e5e61b24e37198349e416f09ef7579f6f25b5725d54df44017e256b1c7060f0c5ba5f3dd162e26fc5fbfcf4294ee261124737b1cdc3024dc2be62c8ebd89c8766bfaf3606a9e7aefa4fd41758498441fe69a967005c66df3ac0551d7b04910c6a9fa272aa6d081defbc2db174f</modulus>
      <exponent>010001</exponent>
    </public-key>
  </catalog>
</root>
```
Copy the public-exponent to the **`<exponent>`** field.

`*` Please be noticed that this key should be pre-distributed.

Now you can upload the encrypted [catalog](PatchesCatalog.md) to the web hosting.

### How do I change the location or file name of client.xml? ###
The default location of [client.xml](ClientSettings.md) is specified by the file stored in the jar named **config**, change the value inside can change the default location of the [client.xml](ClientSettings.md).

Besides, you can pass the path to [client.xml](ClientSettings.md) as the first argument to the **Launcher** and **PatchDownloader**, e.g.:
```
java -jar Launcher.jar config.xml
```
Notice that the argument after **Launcher.jar** cannot have space.