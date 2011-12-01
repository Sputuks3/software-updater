package updater.crypto;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import updater.script.InvalidFormatException;
import updater.util.CommonUtil;

/**
 * The cipher key generator.
 * 
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class KeyGenerator {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected KeyGenerator() {
    }

    /**
     * Generate a RSA key and save to file.
     * 
     * @param keySize the key size in bits, must >= 512 (required by Java).
     * @param saveTo the place to save the generated file
     * 
     * @throws IOException the keySize is invalid or error occurred when writing to file
     * @throws InvalidParameterException {@code keySize} is invalid
     */
    public static void generateRSA(int keySize, File saveTo) throws IOException, InvalidParameterException {
        if (saveTo == null) {
            return;
        }
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize);
            KeyPair keyPair = keyPairGenerator.genKeyPair();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec publicKeySpec = keyFactory.getKeySpec(keyPair.getPublic(), RSAPublicKeySpec.class);
            RSAPrivateKeySpec privateKeySpec = keyFactory.getKeySpec(keyPair.getPrivate(), RSAPrivateKeySpec.class);

            RSAKey rsaKey = new RSAKey(privateKeySpec.getModulus().toByteArray(), publicKeySpec.getPublicExponent().toByteArray(), privateKeySpec.getPrivateExponent().toByteArray());

            CommonUtil.writeFile(saveTo, rsaKey.output());
        } catch (InvalidKeySpecException ex) {
            if (debug) {
                Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NoSuchAlgorithmException ex) {
            if (debug) {
                Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (UnsupportedEncodingException ex) {
            if (debug) {
                Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (TransformerException ex) {
            if (debug) {
                Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Generate a AES key and save to file.
     * 
     * @param keySize the key size in bits
     * @param saveTo the place to save the generated file
     * 
     * @throws IllegalArgumentException {@code keySize} < 8
     * @throws IOException error occurred when writing to file
     */
    public static void generateAES(int keySize, File saveTo) throws IOException {
        if (keySize < 1) {
            throw new IllegalArgumentException("argument 'keySize' must >= 8");
        }
        if (saveTo == null) {
            return;
        }
        byte[] key = generateRandom(keySize / 8);
        byte[] IV = generateRandom(16);

        AESKey aesKey = new AESKey(key, IV);
        try {
            CommonUtil.writeFile(saveTo, aesKey.output());
        } catch (TransformerException ex) {
            if (debug) {
                Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Renew the 128-bit initial vector in the AES key file.
     * 
     * @param file the AES key file
     * 
     * @throws IOException error occurred when reading/writing to the key file
     * @throws InvalidFormatException the format of the content in the key file is invalid
     */
    public static void renewAESIV(File file) throws IOException, InvalidFormatException {
        if (file == null) {
            return;
        }
        byte[] IV = generateRandom(16);

        AESKey aesKey = AESKey.read(CommonUtil.readFile(file));
        aesKey.setIV(IV);

        try {
            CommonUtil.writeFile(file, aesKey.output());
        } catch (TransformerException ex) {
            if (debug) {
                Logger.getLogger(KeyGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Generate a random byte array with specified length in byte.
     * 
     * @param length the length in byte, not allow <= 0
     * 
     * @return the random generated byte array
     */
    public static byte[] generateRandom(int length) {
        byte[] b = new byte[length];
        (new Random()).nextBytes(b);
        return b;
    }
}
