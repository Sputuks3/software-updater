package updater.patch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;
import updater.crypto.AESKey;
import updater.util.CommonUtil;
import watne.seis720.project.AESForFile;
import watne.seis720.project.AESForFileListener;
import watne.seis720.project.KeySize;
import watne.seis720.project.Mode;
import watne.seis720.project.Padding;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchWriteUtil {

    protected PatchWriteUtil() {
    }

    public static void writeHeader(OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException("argument 'out' cannot be null");
        }

        out.write('P');
        out.write('A');
        out.write('T');
        out.write('C');
        out.write('H');
    }

    public static OutputStream writeCompressionMethod(OutputStream out, Compression compression) throws IOException {
        if (out == null) {
            throw new NullPointerException("argument 'out' cannot be null");
        }
        if (compression == null) {
            throw new NullPointerException("argument 'compression' cannot be null");
        }

        out.write(compression.getValue());
        switch (compression) {
            case GZIP:
                return new GZIPOutputStream(out);
            case LZMA2:
                return new XZOutputStream(out, new LZMA2Options());
            default:
                throw new IOException("Compression method not supported/not exist");
        }
    }

    public static void writeXML(OutputStream out, byte[] content) throws IOException {
        if (out == null) {
            throw new NullPointerException("argument 'out' cannot be null");
        }
        if (content == null) {
            throw new NullPointerException("argument 'content' cannot be null");
        }

        int contentLength = content.length;

        out.write((contentLength >> 16) & 0xff);
        out.write((contentLength >> 8) & 0xff);
        out.write(contentLength & 0xff);

        // XML content, max 16MiB
        out.write(content);
    }

    public static void writePatch(File fromFile, OutputStream toStream) throws IOException {
        if (fromFile == null) {
            throw new NullPointerException("argument 'fromFile' cannot be null");
        }
        if (toStream == null) {
            throw new NullPointerException("argument 'toStream' cannot be null");
        }

        FileInputStream fin = null;
        try {
            long fileLength = fromFile.length();

            fin = new FileInputStream(fromFile);

            byte[] b = new byte[32768];
            int byteRead, cumulativeByteRead = 0;
            while ((byteRead = fin.read(b)) != -1) {
                toStream.write(b, 0, byteRead);

                cumulativeByteRead += byteRead;
                if (cumulativeByteRead >= fileLength) {
                    break;
                }
            }

            if (cumulativeByteRead != fileLength) {
                throw new IOException("Number of bytes read not equals to the cumulative number of bytes read, from file: " + fromFile.getAbsolutePath() + ", cumulate: " + cumulativeByteRead + ", expected length: " + fileLength);
            }
        } finally {
            CommonUtil.closeQuietly(fin);
        }
    }

    public static void encrypt(AESKey aesKey, AESForFileListener listener, File patchFile, File encryptTo) throws IOException {
        if (aesKey == null) {
            throw new NullPointerException("argument 'aesKey' cannot be null");
        }
        if (patchFile == null) {
            throw new NullPointerException("argument 'patchFile' cannot be null");
        }
        if (encryptTo == null) {
            throw new NullPointerException("argument 'tempFileForEncryption' cannot be null");
        }

        encryptTo.delete();

        try {
            AESForFile aesCipher = new AESForFile();
            if (listener != null) {
                aesCipher.setListener(listener);
            }
            aesCipher.setMode(Mode.CBC);
            aesCipher.setPadding(Padding.PKCS5PADDING);
            aesCipher.setKeySize(KeySize.BITS256);
            aesCipher.setKey(aesKey.getKey());
            aesCipher.setInitializationVector(aesKey.getIV());
            aesCipher.encryptFile(patchFile, encryptTo);
        } catch (Exception ex) {
            throw new IOException("Error occurred when encrypting the patch: " + ex.getMessage());
        }
    }
}