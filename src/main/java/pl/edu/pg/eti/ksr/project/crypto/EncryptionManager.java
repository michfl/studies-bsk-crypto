package pl.edu.pg.eti.ksr.project.crypto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.*;

/**
 * Manager for all encryption and decryption operations.
 */
public class EncryptionManager {

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
    public EncryptionManager(String transformation) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.cipher = Cipher.getInstance(transformation);
        this.transformation = transformation;
    }
}
