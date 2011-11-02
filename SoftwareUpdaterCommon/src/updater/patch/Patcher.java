package updater.patch;

import com.nothome.delta.GDiffPatcher;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import updater.script.InvalidFormatException;
import updater.script.Patch;
import updater.script.Patch.Operation;
import updater.script.Patch.ValidationFile;
import updater.util.CommonUtil;
import updater.util.InterruptibleInputStream;
import updater.util.InterruptibleOutputStream;
import updater.util.SeekableFile;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Patcher {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }
    protected PatcherListener listener;
    protected PatchLogWriter log;
    protected File tempDir;
    protected String softwareDir;
    private byte[] buf;
    protected float progress;

    public Patcher(PatcherListener listener, PatchLogWriter log, File softwareDir, File tempDir) throws IOException {
        if (listener == null) {
            throw new NullPointerException("argument 'listener' cannot be null");
        }
        if (log == null) {
            throw new NullPointerException("argument 'log' cannot be null");
        }
        if (softwareDir == null) {
            throw new NullPointerException("argument 'softwareDir' cannot be null");
        }
        if (tempDir == null) {
            throw new NullPointerException("argument 'tempDir' cannot be null");
        }

        this.listener = listener;
        this.log = log;

        if (!softwareDir.exists() || !softwareDir.isDirectory()) {
            throw new IOException("software directory not exist or not a directory");
        }
        this.softwareDir = softwareDir.getAbsolutePath() + File.separator;

        this.tempDir = tempDir;
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            throw new IOException("temporary directory not exist or not a directory");
        }

        buf = new byte[32768];
        progress = 0;
    }

    protected void doOperation(Operation operation, InputStream patchIn, File tempNewFile) throws IOException {
        if (operation == null) {
            return;
        }
        if (patchIn == null) {
            throw new NullPointerException("argument 'patchIn' cannot be null");
        }
        if (tempNewFile == null) {
            throw new NullPointerException("argument 'tempNewFile' cannot be null");
        }

        if (operation.getType().equals("remove")) {
            // doOperation will not change/remove all existing 'old files'
            return;
        } else if (operation.getType().equals("new") || operation.getType().equals("force")) {
            if (operation.getFileType().equals("folder")) {
                return;
            }
            listener.patchProgress((int) progress, "Creating new file " + operation.getNewFilePath() + " ...");
        } else {
            // replace or patch
            listener.patchProgress((int) progress, "Patching " + operation.getOldFilePath() + " ...");
        }

        File oldFile = null;
        if (operation.getOldFilePath() != null) {
            // check old file checksum and length
            oldFile = new File(softwareDir + operation.getOldFilePath());
            if (!oldFile.exists()) {
                throw new IOException("Old file not exist: " + softwareDir + operation.getOldFilePath());
            }
            if (!CommonUtil.getSHA256String(oldFile).equals(operation.getOldFileChecksum()) || oldFile.length() != operation.getOldFileLength()) {
                throw new IOException("Checksum or length does not match (old file): " + softwareDir + operation.getOldFilePath());
            }
        }

        // check if it is patched and waiting for move already
        if (tempNewFile.exists() && CommonUtil.getSHA256String(tempNewFile).equals(operation.getNewFileChecksum()) && tempNewFile.length() == operation.getNewFileLength()) {
            long byteSkipped = patchIn.skip(operation.getPatchLength());
            if (byteSkipped != operation.getPatchLength()) {
                throw new IOException("Failed to skip remaining bytes in 'patchIn'.");
            }
            return;
        }

        InterruptibleOutputStream tempNewFileOut = null;
        InterruptibleInputStream interruptiblePatchIn = null;
        RandomAccessFile randomAccessOldFile = null;
        try {
            tempNewFileOut = new InterruptibleOutputStream(new BufferedOutputStream(new FileOutputStream(tempNewFile)));
            interruptiblePatchIn = new InterruptibleInputStream(patchIn, operation.getPatchLength());

            if (operation.getType().equals("patch")) {
                GDiffPatcher diffPatcher = new GDiffPatcher();
                randomAccessOldFile = new RandomAccessFile(oldFile, "r");
                SeekableFile seekableRandomAccessOldFile = new SeekableFile(randomAccessOldFile);

                //<editor-fold defaultstate="collapsed" desc="add interrupted tasks">
                final OutputStream _tempNewFileOut = tempNewFileOut;
                final InterruptibleInputStream _interruptiblePatchIn = interruptiblePatchIn;
                final RandomAccessFile _randomAccessOldFile = randomAccessOldFile;
                Runnable interruptedTask = new Runnable() {

                    @Override
                    public void run() {
                        try {
                            _tempNewFileOut.close();
                            _interruptiblePatchIn.close();
                            _randomAccessOldFile.close();
                        } catch (IOException ex) {
                            if (debug) {
                                Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                };
                tempNewFileOut.addInterruptedTask(interruptedTask);
                interruptiblePatchIn.addInterruptedTask(interruptedTask);
                seekableRandomAccessOldFile.addInterruptedTask(interruptedTask);
                //</editor-fold>

                diffPatcher.patch(seekableRandomAccessOldFile, interruptiblePatchIn, tempNewFileOut);
            } else {
                //<editor-fold defaultstate="collapsed" desc="add interrupted tasks">
                final OutputStream _tempNewFileOut = tempNewFileOut;
                final InterruptibleInputStream _interruptiblePatchIn = interruptiblePatchIn;
                Runnable interruptedTask = new Runnable() {

                    @Override
                    public void run() {
                        try {
                            _tempNewFileOut.close();
                            _interruptiblePatchIn.close();
                        } catch (IOException ex) {
                            if (debug) {
                                Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                };
                tempNewFileOut.addInterruptedTask(interruptedTask);
                interruptiblePatchIn.addInterruptedTask(interruptedTask);
                //</editor-fold>

                // replace, new or force
                int byteRead, remaining = operation.getPatchLength();
                while (true) {
                    if (remaining <= 0) {
                        break;
                    }

                    int lengthToRead = buf.length > remaining ? remaining : buf.length;
                    byteRead = interruptiblePatchIn.read(buf, 0, lengthToRead);
                    if (byteRead == -1) {
                        break;
                    }
                    tempNewFileOut.write(buf, 0, byteRead);
                    remaining -= byteRead;
                }
            }
        } finally {
            try {
                if (randomAccessOldFile != null) {
                    randomAccessOldFile.close();
                }
                if (interruptiblePatchIn != null) {
                    long byteSkipped = patchIn.skip(interruptiblePatchIn.remaining());
                    if (byteSkipped != interruptiblePatchIn.remaining()) {
                        throw new IOException("Failed to skip remaining bytes in 'interruptiblePatchIn'.");
                    }
                }
                if (tempNewFileOut != null) {
                    tempNewFileOut.close();
                }
            } catch (IOException ex) {
                if (debug) {
                    Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        // check new file checksum and length
        if (!operation.getType().equals("new")) {
            String tempNewFileSHA256 = CommonUtil.getSHA256String(tempNewFile);
            if (!tempNewFileSHA256.equals(operation.getNewFileChecksum()) || tempNewFile.length() != operation.getNewFileLength()) {
                throw new IOException("Checksum or length does not match (new file): " + tempNewFile.getAbsolutePath() + ", old file path: " + softwareDir + operation.getOldFilePath() + ", expected checksum: " + operation.getNewFileChecksum() + ", actual checksum: " + tempNewFileSHA256 + ", expected length: " + operation.getNewFileLength() + ", actual length: " + tempNewFile.length());
            }
        }
    }

    protected void tryAcquireExclusiveLocks(List<Operation> operations, int startFromFileIndex) throws IOException {
        for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
            Operation _operation = operations.get(i);

            if (_operation.getOldFilePath() != null) {
                if (!CommonUtil.tryLock(new File(softwareDir + _operation.getOldFilePath()))) {
                    throw new IOException("Failed to acquire lock on (old file): " + softwareDir + _operation.getOldFilePath());
                }
            }

//            if (_operation.getNewFilePath() != null) {
//                if (!CommonUtil.tryLock(new File(_operation.getNewFilePath()))) {
//                    throw new IOException("Failed to acquire lock on (new file): " + _operation.getNewFilePath());
//                }
//            }
        }
    }

    protected void doReplacement(List<Operation> operations, int startFromFileIndex, float progressOccupied) throws IOException {
        if (operations == null) {
            return;
        }

        float progressStep = progressOccupied / (float) operations.size();
        progress += startFromFileIndex * progressStep;

        for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
            Operation _operation = operations.get(i);

            log.logPatch(PatchLogWriter.Action.START, i, PatchLogWriter.OperationType.get(_operation.getType()), _operation.getOldFilePath(), _operation.getNewFilePath());

            if (_operation.getType().equals("remove")) {
                listener.patchProgress((int) progress, "Removing " + _operation.getOldFilePath() + " ...");
                new File(softwareDir + _operation.getOldFilePath()).delete();
            } else if (_operation.getType().equals("new") || _operation.getType().equals("force")) {
                listener.patchProgress((int) progress, "Copying new file to " + _operation.getNewFilePath() + " ...");
                if (_operation.getFileType().equals("folder")) {
                    File newFolder = new File(softwareDir + _operation.getNewFilePath());
                    if (!newFolder.isDirectory() && !newFolder.mkdirs()) {
                        throw new IOException("Create folder failed: " + softwareDir + _operation.getNewFilePath());
                    }
                } else {
                    File newFile = new File(softwareDir + _operation.getNewFilePath());
                    new File(CommonUtil.getFileDirectory(newFile)).mkdirs();
                    newFile.delete();
                    new File(tempDir + File.separator + i).renameTo(newFile);
                }
            } else {
                // patch or replace
                listener.patchProgress((int) progress, "Copying from " + _operation.getOldFilePath() + " to " + _operation.getNewFilePath() + " ...");
                new File(softwareDir + _operation.getNewFilePath()).delete();
                new File(tempDir + File.separator + i).renameTo(new File(softwareDir + _operation.getNewFilePath()));
            }

            log.logPatch(PatchLogWriter.Action.FINISH, i, PatchLogWriter.OperationType.get(_operation.getType()), _operation.getOldFilePath(), _operation.getNewFilePath());
            progress += progressStep;
        }
    }

    public boolean doPatch(File patchFile, int patchId, int startFromFileIndex) throws IOException {
        if (patchFile == null) {
            return true;
        }

        if (!patchFile.exists() || patchFile.isDirectory()) {
            throw new IOException("patch file not exist or not a file");
        }

        boolean returnResult = true;

        InputStream patchIn = null;
        try {
            patchIn = new BufferedInputStream(new FileInputStream(patchFile));

            progress = 0;
            listener.patchProgress((int) progress, "Preparing new patch ...");
            listener.patchEnableCancel(false);
            // header
            PatchReadUtil.readHeader(patchIn);
            InputStream decompressedPatchIn = PatchReadUtil.readCompressionMethod(patchIn);
            Patch patch = PatchReadUtil.readXML(decompressedPatchIn);

            List<Operation> operations = patch.getOperations();
            List<ValidationFile> validations = patch.getValidations();

            // start log
            log.logStart(patchId, patch.getVersionFrom(), patch.getVersionTo());

            progress = 5;
            listener.patchProgress((int) progress, "Updating ...");
            listener.patchEnableCancel(true);
            // start patch - patch files and store to temporary directory first
            float progressStep = 70.0F / (float) operations.size();
            progress += startFromFileIndex * progressStep;
            for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
                Operation _operation = operations.get(i);
                doOperation(_operation, decompressedPatchIn, new File(tempDir + File.separator + i));
                progress += progressStep;
            }

            progress = 75;
            listener.patchProgress((int) progress, "Checking the accessibility of all files ...");
            // try acquire locks on all files
            tryAcquireExclusiveLocks(operations, startFromFileIndex);

            progress = 76;
            listener.patchProgress((int) progress, "Replacing old files with new files ...");
            listener.patchEnableCancel(false);
            // all files has patched to temporary directory, replace old files with the new one
            doReplacement(operations, startFromFileIndex, 4.0F);

            progress = 80;
            listener.patchProgress((int) progress, "Validating files ...");
            // validate files
            progressStep = 20.0F / (float) validations.size();
            for (ValidationFile _validationFile : validations) {
                listener.patchProgress((int) progress, "Validating file: " + _validationFile.getFilePath());

                File _file = new File(softwareDir + _validationFile.getFilePath());
                if (_validationFile.getFileLength() == -1) {
                    if (!_file.isDirectory()) {
                        throw new IOException("Folder missed: " + softwareDir + _validationFile.getFilePath());
                    }
                } else {
                    if (!_file.exists()) {
                        throw new IOException("File missed: " + softwareDir + _validationFile.getFilePath());
                    }
                    if (_file.length() != _validationFile.getFileLength()) {
                        throw new IOException("File length not matched, file: " + softwareDir + _validationFile.getFilePath() + ", expected: " + _validationFile.getFileLength() + ", found: " + _file.length());
                    }
                    if (!CommonUtil.getSHA256String(_file).equals(_validationFile.getFileChecksum())) {
                        throw new IOException("File checksum incorrect: " + softwareDir + _validationFile.getFilePath());
                    }
                }

                progress += progressStep;
            }

            listener.patchProgress(100, "Finished.");
            log.logEnd();
        } catch (InvalidFormatException ex) {
            if (debug) {
                Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            try {
                if (patchIn != null) {
                    patchIn.close();
                }
            } catch (IOException ex) {
                if (debug) {
                    Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        listener.patchFinished(returnResult);

        return returnResult;
    }
}