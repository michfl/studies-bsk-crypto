package pl.edu.pg.eti.ksr.project.crypto;

import lombok.Getter;
import lombok.Setter;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Manager for all encryption and decryption operations.
 */
public class EncryptionManager {

    @Getter
    private final BlockingDeque<byte[]> encryptionBuffer;

    @Getter
    @Setter
    private int bufferSize;

    @Getter
    private String transformation;

    private Cipher cipher;

    /**
     * Sets new transformation for next operations.
     * @param transformation new transformation
     * @throws NoSuchPaddingException wrong padding setting passed
     * @throws NoSuchAlgorithmException wrong algorithm setting passed
     */
    public void setTransformation(String transformation) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.cipher = Cipher.getInstance(transformation);
        this.transformation = transformation;
    }

    /**
     * Performs desired ciphering operation with a given mode.
     * @param opMode encryption or decryption mode
     * @param input input buffer
     * @param key key for ciphering
     * @param iv IV for encrypting
     * @return cyphered buffer
     * @throws InvalidKeyException incorrect key passed, wrong format
     * @throws IllegalBlockSizeException problem with block size
     * @throws BadPaddingException problem with padding
     * @throws InvalidAlgorithmParameterException problem with provided IV
     */
    public byte[] encrypt(int opMode, byte[] input, Key key, IvParameterSpec iv)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {

        cipher.init(opMode, key, iv);
        return cipher.doFinal(input);
    }

    /**
     * Performs desired ciphering operation with a given mode.
     * @param opMode encryption or decryption mode
     * @param input input buffer
     * @param key key for ciphering
     * @return cyphered buffer
     * @throws InvalidKeyException incorrect key passed, wrong format
     * @throws IllegalBlockSizeException problem with block size
     * @throws BadPaddingException problem with padding
     */
    public byte[] encrypt(int opMode, byte[] input, Key key)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        cipher.init(opMode, key);
        return cipher.doFinal(input);
    }

    public void encrypt(Path source, Path target, Key key, IvParameterSpec iv)
            throws InvalidAlgorithmParameterException, InvalidKeyException {

        cipher.init(Cipher.ENCRYPT_MODE, key, iv);


    }

    /**
     * Encrypts provided text.
     * @param text text for encryption
     * @param key key for encryption
     * @param iv IV for encrypting
     * @return cipher text
     * @throws InvalidKeyException incorrect key passed, wrong format
     * @throws IllegalBlockSizeException incorrect key passed, wrong format
     * @throws BadPaddingException problem with padding
     * @throws InvalidAlgorithmParameterException problem with provided IV
     */
    public byte[] encrypt(String text, Key key, IvParameterSpec iv)
            throws InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException {

        return encrypt(Cipher.ENCRYPT_MODE, text.getBytes(), key, iv);
    }

    /**
     * Encrypts provided text.
     * @param text text for encryption
     * @param key key for encryption
     * @return cipher text
     * @throws InvalidKeyException incorrect key passed, wrong format
     * @throws IllegalBlockSizeException incorrect key passed, wrong format
     * @throws BadPaddingException problem with padding
     */
    public byte[] encrypt(String text, Key key)
            throws IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException {

        return encrypt(Cipher.ENCRYPT_MODE, text.getBytes(), key);
    }

    /**
     * Decrypts provided cipher text.
     * @param cipherText cipher text for decryption
     * @param key key for decryption
     * @param iv IV for decrypting
     * @return plain text
     * @throws InvalidKeyException incorrect key passed, wrong format
     * @throws IllegalBlockSizeException incorrect key passed, wrong format
     * @throws BadPaddingException problem with padding
     * @throws InvalidAlgorithmParameterException problem with provided IV
     */
    public String decrypt(byte[] cipherText, Key key, IvParameterSpec iv)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {

        return new String(encrypt(Cipher.DECRYPT_MODE, cipherText, key, iv), StandardCharsets.UTF_8);
    }

    /**
     * Decrypts provided cipher text.
     * @param cipherText cipher text for decryption
     * @param key key for decryption
     * @return plain text
     * @throws InvalidKeyException incorrect key passed, wrong format
     * @throws IllegalBlockSizeException incorrect key passed, wrong format
     * @throws BadPaddingException problem with padding
     */
    public String decrypt(byte[] cipherText, Key key)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        return new String(encrypt(Cipher.DECRYPT_MODE, cipherText, key), StandardCharsets.UTF_8);
    }

    /**
     * Method for generating secure key.
     * @param keySize key size in bits
     * @param algorithm algorithm to be used
     * @return generated key
     * @throws NoSuchAlgorithmException incorrect algorithm provided
     */
    public static Key generateKey(int keySize, String algorithm) throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
        keyGen.init(keySize);
        return keyGen.generateKey();
    }

    /**
     * Method for generating a pair of public and private keys.
     * @param keySize key size in bits
     * @param algorithm algorithm to be used
     * @return generated key pair
     * @throws NoSuchAlgorithmException incorrect algorithm provided
     */
    public static KeyPair generateKeyPair(int keySize, String algorithm) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(algorithm);
        keyPairGen.initialize(keySize);
        return keyPairGen.generateKeyPair();
    }

    /**
     * Method for generating IVs.
     * @param blockSize block size used later during cyphering
     * @return generated IV as parameter spec
     */
    public static IvParameterSpec generateIv(int blockSize) {
        byte[] iv = new byte[blockSize];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    /**
     * Encryption manager constructor.
     * @param transformation desired transformation to be used
     * @throws NoSuchPaddingException wrong padding setting passed
     * @throws NoSuchAlgorithmException wrong algorithm setting passed
     */
    public EncryptionManager(String transformation, int bufferSize) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.encryptionBuffer = new LinkedBlockingDeque<>();
        this.cipher = Cipher.getInstance(transformation);
        this.transformation = transformation;
        this.bufferSize = bufferSize;
    }
}
