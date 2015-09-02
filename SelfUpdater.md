# Self Updater #



Self Updater is used by software launcher.
When the launcher encounter any files that it can't do a replacement due to file locking, then this will be used.
The launcher will launch this and exit (to release the file lock on the itself), then this self updater will do the replacement according to a list the launcher give it.

## Launch ##
```
java -jar SelfUpdater.jar {lock file} {replacement file} {launch command}
```
  * **{lock file}** is the path to the lock file
  * **{replacement file}** is the path of text file that contain the replacement information, see [here](#Replacement_File.md)
  * **{launch command}** is the command to use to start the Launcher, it contain JVM arguments and run arguments (can take up more than one argument)

## Flow ##
  1. Acquire update lock
  1. Do files replacement, see [replacement file](#Replacement_File.md)
  1. Start the launcher and exit
If step 1 or 2 failed, it will retry until succeed or [maximum execution time](#Maximum_Execution_Time.md) reached.

## Appendix ##

### Replacement File ###
The format of the replacement file:

One row for destination file path (0), one row for new file path (1), one row for the path to place/move the destination file (2).<br />
Flow: 0->2, 1->0

Example:
```
C:\software\dest.txt
C:\tmp\1.tmp
C:\tmp\1.old
C:\software\dest.jar
C:\tmp\2.tmp
C:\tmp\2.old
```
Note there is a new line character at the end.

### Maximum Execution Time ###
The time is in ms, default value is 15000 (15 seconds).
When the self updater failed, it will not give up immediately, because the launcher may not exit so quickly after launching this.
In this case, the program will keep trying until this maximum execution time is reached.
This is configurable by replacing/editing '/config.xml' inside the jar, for more information, see the code below.