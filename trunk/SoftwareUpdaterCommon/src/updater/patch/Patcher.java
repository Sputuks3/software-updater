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

    protected PatcherListener listener;
    protected PatchLogWriter log;
    protected File patchFile;
    protected File tempDir;
    protected String baseDir;
    private byte[] buf;
    protected float progress;

    public Patcher(PatcherListener listener, PatchLogWriter log, File patchFile, File baseDir, File tempDir) throws IOException {
        this.listener = listener;
        this.log = log;

        this.patchFile = patchFile;
        if (baseDir == null || !baseDir.isDirectory()) {
            this.baseDir = new File("").getAbsolutePath() + File.separator;
        } else {
            this.baseDir = baseDir.getAbsolutePath() + File.separator;
        }
        if (!patchFile.exists() || patchFile.isDirectory()) {
            throw new IOException("patch file not exist or not a file");
        }

        this.tempDir = tempDir;
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            throw new IOException("temporary directory not exist or not a directory");
        }

        buf = new byte[32768];
        progress = 0;
    }

    public void close() {
        if (log != null) {
            try {
                log.close();
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
    }

    protected void doOperation(Operation operation, InputStream patchIn, File tempNewFile) throws IOException {
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
            oldFile = new File(baseDir + operation.getOldFilePath());
            if (!oldFile.exists()) {
                throw new IOException("Old file not exist: " + baseDir + operation.getOldFilePath());
            }
            if (!CommonUtil.getSHA256String(oldFile).equals(operation.getOldFileChecksum()) || oldFile.length() != operation.getOldFileLength()) {
                throw new IOException("Checksum or length does not match (old file): " + baseDir + operation.getOldFilePath());
            }
        }

        // check if it is patched and waiting for move already
        if (tempNewFile.exists() && CommonUtil.getSHA256String(tempNewFile).equals(operation.getNewFileChecksum()) && tempNewFile.length() == operation.getNewFileLength()) {
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
                            System.err.println(ex);
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
                            System.err.println(ex);
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
                System.err.println(ex);
            }
        }

        // check new file checksum and length
        if (!operation.getType().equals("new")) {
            String tempNewFileSHA256 = CommonUtil.getSHA256String(tempNewFile);
            if (!tempNewFileSHA256.equals(operation.getNewFileChecksum()) || tempNewFile.length() != operation.getNewFileLength()) {
                throw new IOException("Checksum or length does not match (new file): " + tempNewFile.getAbsolutePath() + ", old file path: " + baseDir + operation.getOldFilePath() + ", expected checksum: " + operation.getNewFileChecksum() + ", actual checksum: " + tempNewFileSHA256 + ", expected length: " + operation.getNewFileLength() + ", actual length: " + tempNewFile.length());
            }
        }
    }

    protected void tryAcquireExclusiveLocks(List<Operation> operations, int startFromFileIndex) throws IOException {
        for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
            Operation _operation = operations.get(i);

            if (_operation.getOldFilePath() != null) {
                if (!CommonUtil.tryLock(new File(baseDir + _operation.getOldFilePath()))) {
                    throw new IOException("Failed to acquire lock on (old file): " + baseDir + _operation.getOldFilePath());
                }
            }

//            if (_operation.getNewFilePath() != null) {
//                if (!CommonUtil.tryLock(new File(_operation.getNewFilePath()))) {
//                    throw new IOException("Failed to acquire lock on (new file): " + _operation.getNewFilePath());
//                }
//            }
        }
    }

    protected void doReplacement(List<Operation> operations, int startFromFileIndex, String patchFileAbsolutePath, Patch patch, float progressOccupied) throws IOException {
        float progressStep = progressOccupied / (float) operations.size();
        progress += startFromFileIndex * progressStep;

        for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
            Operation _operation = operations.get(i);

            log.logPatch(PatchLogWriter.Action.START, i, PatchLogWriter.OperationType.get(_operation.getType()), _operation.getOldFilePath(), _operation.getNewFilePath());

            if (_operation.getType().equals("remove")) {
                listener.patchProgress((int) progress, "Removing " + _operation.getOldFilePath() + " ...");
                new File(baseDir + _operation.getOldFilePath()).delete();
            } else if (_operation.getType().equals("new") || _operation.getType().equals("force")) {
                listener.patchProgress((int) progress, "Copying new file to " + _operation.getNewFilePath() + " ...");
                if (_operation.getFileType().equals("folder")) {
                    File newFolder = new File(baseDir + _operation.getNewFilePath());
                    if (!newFolder.isDirectory() && !newFolder.mkdirs()) {
                        throw new IOException("Create folder failed: " + baseDir + _operation.getNewFilePath());
                    }
                } else {
                    File newFile = new File(baseDir + _operation.getNewFilePath());
                    new File(CommonUtil.getFileDirectory(newFile)).mkdirs();
                    newFile.delete();
                    new File(tempDir + File.separator + i).renameTo(newFile);
                }
            } else {
                // patch or replace
                listener.patchProgress((int) progress, "Copying from " + _operation.getOldFilePath() + " to " + _operation.getNewFilePath() + " ...");
                new File(baseDir + _operation.getNewFilePath()).delete();
                new File(tempDir + File.separator + i).renameTo(new File(baseDir + _operation.getNewFilePath()));
            }

            log.logPatch(PatchLogWriter.Action.FINISH, i, PatchLogWriter.OperationType.get(_operation.getType()), _operation.getOldFilePath(), _operation.getNewFilePath());
            progress += progressStep;
        }
    }

    public boolean doPatch(int patchId, int startFromFileIndex) throws IOException {
        boolean returnResult = true;

        InputStream patchIn = null;
        try {
            String patchFileAbsolutePath = patchFile.getAbsolutePath();
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
            doReplacement(operations, startFromFileIndex, patchFileAbsolutePath, patch, 4.0F);

            progress = 80;
            listener.patchProgress((int) progress, "Validating files ...");
            // validate files
            progressStep = 20.0F / (float) validations.size();
            for (ValidationFile _validationFile : validations) {
                listener.patchProgress((int) progress, "Validating file: " + _validationFile.getFilePath());

                File _file = new File(baseDir + _validationFile.getFilePath());
                if (_validationFile.getFileLength() == -1) {
                    if (!_file.isDirectory()) {
                        throw new IOException("Folder missed: " + baseDir + _validationFile.getFilePath());
                    }
                } else {
                    if (!_file.exists()) {
                        throw new IOException("File missed: " + baseDir + _validationFile.getFilePath());
                    }
                    if (_file.length() != _validationFile.getFileLength()) {
                        throw new IOException("File length not matched, file: " + baseDir + _validationFile.getFilePath() + ", expected: " + _validationFile.getFileLength() + ", found: " + _file.length());
                    }
                    if (!CommonUtil.getSHA256String(_file).equals(_validationFile.getFileChecksum())) {
                        throw new IOException("File checksum incorrect: " + baseDir + _validationFile.getFilePath());
                    }
                }

                progress += progressStep;
            }

            listener.patchProgress(100, "Finished.");
            log.logEnd();
        } catch (InvalidFormatException ex) {
            System.err.println(ex);
        } finally {
            try {
                if (patchIn != null) {
                    patchIn.close();
                }
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }

        listener.patchFinished(returnResult);

        return returnResult;
    }
}