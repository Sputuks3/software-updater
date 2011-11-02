package updater.patch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.tukaani.xz.XZOutputStream;
import updater.crypto.AESKey;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.util.CommonUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchPacker {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected PatchPacker() {
    }

    public static void pack(File sourceFolder, File saveToFile, AESKey aesKey, File tempFileForEncryption) throws IOException, InvalidFormatException {
        if (sourceFolder == null) {
            return;
        }
        if (saveToFile == null) {
            throw new NullPointerException("argument 'saveToFile' cannot be null");
        }
        if (aesKey != null && tempFileForEncryption == null) {
            throw new NullPointerException("argument 'tempFileForEncryption' cannot be null while argument 'aesKey' is not null");
        }

        if (!sourceFolder.isDirectory()) {
            throw new IOException("sourceFolder is not a directory.");
        }

        File patchFile = new File(sourceFolder.getAbsolutePath() + File.separator + "patch.xml");
        Patch patch = Patch.read(CommonUtil.readFile(patchFile));

        String sourceFolderPath = sourceFolder.getAbsolutePath();

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(saveToFile);

            PatchWriteUtil.writeHeader(fout);
            XZOutputStream xzOut = (XZOutputStream) PatchWriteUtil.writeCompressionMethod(fout, PatchWriteUtil.Compression.LZMA2);
            try {
                PatchWriteUtil.writeXML(xzOut, patch.output());
            } catch (TransformerException ex) {
                throw new IOException("patch.xml format invalid");
            }

            int operationIdCounter = 1;
            List<Operation> operations = patch.getOperations();
            for (Operation operation : operations) {
                if (operation.getPatchLength() > 0) {
                    try {
                        PatchWriteUtil.writePatch(new File(sourceFolderPath + File.separator + operationIdCounter), xzOut);
                    } catch (IOException ex) {
                        throw new IOException("Patch with id " + operationIdCounter + " not exist.");
                    }
                }
                operationIdCounter++;
            }

            xzOut.finish();
        } finally {
            if (fout != null) {
                fout.close();
            }
        }

        if (aesKey != null) {
            PatchWriteUtil.encrypt(aesKey, saveToFile, tempFileForEncryption);

            saveToFile.delete();
            tempFileForEncryption.renameTo(saveToFile);
        }
    }
}