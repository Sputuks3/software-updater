package updater.launcher;

import java.awt.Image;
import java.awt.Toolkit;
import java.util.logging.Level;
import java.util.logging.Logger;
import updater.launcher.util.Util;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import updater.launcher.patch.BatchPatcher;
import updater.launcher.patch.BatchPatcher.UpdateResult;
import updater.script.Client;
import updater.script.Client.Information;
import updater.script.InvalidFormatException;
import updater.util.CommonUtil.GetClientScriptResult;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareStarter {

    protected SoftwareStarter() {
    }

    public static void start(String clientScriptPath, String[] args) throws IOException, InvalidFormatException, LaunchFailedException {
        File clientScript = new File(clientScriptPath);
        Client client = Client.read(Util.readFile(clientScript));
        if (client != null) {
            start(clientScript, client, args);
        }
    }

    public static void start(File clientScriptFile, Client client, String[] args) throws IOException, InvalidFormatException, LaunchFailedException {
        String jarPath = client.getJarPath();
        String mainClass = client.getMainClass();
        String storagePath = client.getStoragePath();

        Information clientInfo = client.getInformation();

        Image softwareIcon;
        if (clientInfo.getSoftwareIconLocation().equals("jar")) {
            URL resourceURL = SoftwareStarter.class.getResource(clientInfo.getSoftwareIconPath());
            if (resourceURL != null) {
                softwareIcon = Toolkit.getDefaultToolkit().getImage(resourceURL);
            } else {
                throw new IOException("Resource not found: " + clientInfo.getSoftwareIconPath());
            }
        } else {
            softwareIcon = ImageIO.read(new File(clientInfo.getSoftwareIconPath()));
        }
        Image updaterIcon;
        if (clientInfo.getUpdaterIconLocation().equals("jar")) {
            URL resourceURL = SoftwareStarter.class.getResource(clientInfo.getUpdaterIconPath());
            if (resourceURL != null) {
                updaterIcon = Toolkit.getDefaultToolkit().getImage(resourceURL);
            } else {
                throw new IOException("Resource not found: " + clientInfo.getUpdaterIconPath());
            }
        } else {
            updaterIcon = ImageIO.read(new File(clientInfo.getUpdaterIconPath()));
        }

        UpdateResult updateResult = BatchPatcher.update(clientScriptFile, client, new File(storagePath), clientInfo.getSoftwareName(), softwareIcon, clientInfo.getUpdaterTitle(), updaterIcon);
        if (updateResult.isUpdateSucceed() || updateResult.isLaunchSoftware()) {
            startSoftware(jarPath, mainClass, args);
        }
    }

    protected static void startSoftware(String jarPath, String mainClass, String[] args) throws LaunchFailedException {
        try {
            ClassLoader loader = URLClassLoader.newInstance(new URL[]{new File(jarPath).toURI().toURL()}, SoftwareStarter.class.getClassLoader());
            Class<?> clazz = Class.forName(mainClass, true, loader);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("main")) {
                    method.invoke(null, (Object) (args));
                }
            }
        } catch (Exception ex) {
            throw new LaunchFailedException();
        }
    }

    public static void main(String[] args) {
        Util.setLookAndFeel();
        try {
            GetClientScriptResult result = Util.getClientScript(args.length > 0 ? args[0] : null);
            if (result.getClientScript() != null) {
                SoftwareStarter.start(new File(result.getClientScriptPath()), result.getClientScript(), args);
            } else {
                JOptionPane.showMessageDialog(null, "Config file not found, is empty or is invalid.");
            }
        } catch (IOException ex) {
            Logger.getLogger(SoftwareStarter.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Fail to read images stated in the config file: root->information->software-icon or root->information->updater-icon.");
        } catch (InvalidFormatException ex) {
            Logger.getLogger(SoftwareStarter.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Config file format invalid.");
        } catch (LaunchFailedException ex) {
            Logger.getLogger(SoftwareStarter.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Failed to launch the software.");
        }
    }
}