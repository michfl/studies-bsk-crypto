package pl.edu.pg.eti.ksr.project.crypto;

import lombok.Getter;
import pl.edu.pg.eti.ksr.project.observer.Observer;
import pl.edu.pg.eti.ksr.project.observer.Subject;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager for all encryption and decryption operations.
 */
public class EncryptionManager implements Subject {

    /**
     * List of all observers subscribing to this object.
     */
    private final Queue<Observer> observers;

    /**
     * String representation of transformation used in initialization of Cipher object.
     */
    @Getter
    private String transformation;

    /**
     * Cipher object used during encryption and decryption.
     */
    @Getter
    private Cipher cipher;

    /**
     * Flag for stopping running threads.
     */
    @Getter
    private final AtomicBoolean running;

    /**
     * Reference to the currently running thread.
     */
    @Getter
    private Thread encryptorThread;

    @Override
    public void attach(Observer observer) {
        this.observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        this.observers.remove(observer);
    }

    @Override
    public void notifyObs(Object o) {
        for (Observer observer : this.observers) {
            observer.update(o);
        }
    }

    /**
     * Internal method used for notifying observers on the state of encryption / decryption.
     * @param state fractional number between [0, 1]
     */
    void publishEncryptionState(double state) {
        notifyObs(state);
    }

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

    /**
     * Encrypts provided file and outputs to another file.
     * @param source path to a file to be encrypted
     * @param target path to a file that will consist ciphered input file
     * @param key key for encryption
     * @param iv IV for encrypting
     * @param fileSize size of a file in bytes
     * @throws InvalidAlgorithmParameterException problem with provided IV
     * @throws InvalidKeyException incorrect key passed, wrong format
     */
    public void encrypt(Path source, Path target, Key key, IvParameterSpec iv, long fileSize)
            throws InvalidAlgorithmParameterException, InvalidKeyException {

        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        running.set(true);
        encryptorThread = new Thread(
                new FileToFileEncryptor(cipher, source, target, running, fileSize, this));
        encryptorThread.start();
    }

    /**
     * Encrypts provided file and outputs to another file.
     * @param source path to a file to be encrypted
     * @param target path to a file that will consist ciphered input file
     * @param key key for encryption
     * @param fileSize size of a file in bytes
     * @throws InvalidKeyException incorrect key passed, wrong format
     */
    public void encrypt(Path source, Path target, Key key, long fileSize)
            throws InvalidKeyException {

        cipher.init(Cipher.ENCRYPT_MODE, key);

        running.set(true);
        encryptorThread = new Thread(
                new FileToFileEncryptor(cipher, source, target, running, fileSize, this));
        encryptorThread.start();
    }

    /**
     * Encrypts provided file and outputs to provided queue.
     * @param source path to a file to be encrypted
     * @param target queue to which encrypted data will be inserted
     * @param key key for encryption
     * @param iv IV for encrypting
     * @param fileSize size of a file in bytes
     * @throws InvalidAlgorithmParameterException problem with provided IV
     * @throws InvalidKeyException incorrect key passed, wrong format
     */
    public void encrypt(Path source, BlockingQueue<byte[]> target, Key key, IvParameterSpec iv, long fileSize)
            throws InvalidAlgorithmParameterException, InvalidKeyException {

        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        running.set(true);
        encryptorThread = new Thread(
                new FileToBlockingQueueEncryptor(cipher, source, target, running, fileSize, this));
        encryptorThread.start();
    }

    /**
     * Encrypts provided file and outputs to provided queue.
     * @param source path to a file to be encrypted
     * @param target queue to which encrypted data will be inserted
     * @param key key for encryption
     * @param fileSize size of a file in bytes
     * @throws InvalidKeyException incorrect key passed, wrong format
     */
    public void encrypt(Path source, BlockingQueue<byte[]> target, Key key, long fileSize)
            throws InvalidKeyException {

        cipher.init(Cipher.ENCRYPT_MODE, key);

        running.set(true);
        encryptorThread = new Thread(
                new FileToBlockingQueueEncryptor(cipher, source, target, running, fileSize, this));
        encryptorThread.start();
    }

    /**
     * Encrypts provided text.
     * @param text text for encryption
     * @param key key for encryption
     * @param iv IV for encrypting
     * @return cipher text
     * @throws InvalidKeyException incorrect key passed, wrong format
     * @throws IllegalBlockSizeException problem with block size
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
     * @throws IllegalBlockSizeException problem with block size
     * @throws BadPaddingException problem with padding
     */
    public byte[] encrypt(String text, Key key)
            throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

        return encrypt(Cipher.ENCRYPT_MODE, text.getBytes(), key);
    }

    /**
     * Encrypts provided key.
     * @param toEncrypt key to encrypt
     * @param key key for encryption
     * @return encrypted key in a byte array format
     * @throws IllegalBlockSizeException problem with block size
     * @throws BadPaddingException problem with padding
     * @throws InvalidKeyException incorrect key passed, wrong format
     */
    public byte[] encrypt(Key toEncrypt, Key key)
            throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

        return encrypt(Cipher.ENCRYPT_MODE, toEncrypt.getEncoded(), key);
    }

    /**
     * Decrypts key in a byte array format.
     * @param toDecrypt key in a byte array format
     * @param key key for decryption
     * @param algorithm algorithm used for creation of original encrypted key
     * @return decrypted key
     * @throws IllegalBlockSizeException problem with block size
     * @throws BadPaddingException problem with padding
     * @throws InvalidKeyException incorrect key passed, wrong format
     */
    public Key decrypt(byte[] toDecrypt, Key key, String algorithm)
            throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

        byte[] decrypted = encrypt(Cipher.DECRYPT_MODE, toDecrypt, key);

        return new SecretKeySpec(decrypted, 0, decrypted.length, algorithm);
    }

    /**
     * Decrypts provided file and outputs to another file.
     * @param source path to a file to be decrypted
     * @param target path to a file that will consist decrypted input file
     * @param key key for decryption
     * @param iv IV for decrypting
     * @param fileSize size of an original file in bytes
     * @throws InvalidAlgorithmParameterException problem with provided IV
     * @throws InvalidKeyException incorrect key passed, wrong format
     */
    public void decrypt(Path source, Path target, Key key, IvParameterSpec iv, long fileSize)
            throws InvalidAlgorithmParameterException, InvalidKeyException {

        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        running.set(true);
        encryptorThread = new Thread(
                new FileToFileEncryptor(cipher, source, target, running, fileSize, this));
        encryptorThread.start();
    }

    /**
     * Decrypts provided file and outputs to another file.
     * @param source path to a file to be decrypted
     * @param target path to a file that will consist decrypted input file
     * @param key key for decryption
     * @param fileSize size of an original file in bytes
     * @throws InvalidKeyException incorrect key passed, wrong format
     */
    public void decrypt(Path source, Path target, Key key, long fileSize)
            throws InvalidKeyException {

        cipher.init(Cipher.DECRYPT_MODE, key);

        running.set(true);
        encryptorThread = new Thread(
                new FileToFileEncryptor(cipher, source, target, running, fileSize, this));
        encryptorThread.start();
    }

    /**
     * Decrypts contents of a blocking queue to a file.
     * @param source queue with encrypted data
     * @param target path where decrypted file will be saved
     * @param key key for decryption
     * @param iv IV for decryption
     * @param fileSize size of an original file in bytes
     * @throws InvalidAlgorithmParameterException problem with provided IV
     * @throws InvalidKeyException incorrect key passed, wrong format
     */
    public void decrypt(BlockingQueue<byte[]> source, Path target, Key key, IvParameterSpec iv, long fileSize)
            throws InvalidAlgorithmParameterException, InvalidKeyException {

        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        running.set(true);
        encryptorThread = new Thread(
                new BlockingQueueToFileEncryptor(cipher, source, target, running, fileSize, this));
        encryptorThread.start();
    }

    /**
     * Decrypts contents of a blocking queue to a file.
     * @param source queue with encrypted data
     * @param target path where decrypted file will be saved
     * @param key key for decryption
     * @param fileSize size of an original file in bytes
     * @throws InvalidKeyException incorrect key passed, wrong format
     */
    public void decrypt(BlockingQueue<byte[]> source, Path target, Key key, long fileSize)
            throws InvalidKeyException {

        cipher.init(Cipher.DECRYPT_MODE, key);

        running.set(true);
        encryptorThread = new Thread(
                new BlockingQueueToFileEncryptor(cipher, source, target, running, fileSize, this));
        encryptorThread.start();
    }

    /**
     * Decrypts provided cipher text.
     * @param cipherText cipher text for decryption
     * @param key key for decryption
     * @param iv IV for decrypting
     * @return plain text
     * @throws InvalidKeyException incorrect key passed, wrong format
     * @throws IllegalBlockSizeException problem with block size
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
     * @throws IllegalBlockSizeException problem with block size
     * @throws BadPaddingException problem with padding
     */
    public String decrypt(byte[] cipherText, Key key)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        return new String(encrypt(Cipher.DECRYPT_MODE, cipherText, key), StandardCharsets.UTF_8);
    }

    /**
     * Stops currently running threads.
     * Can be used for stopping file encryption or decryption tasks.
     */
    public void stopCurrentWork() {
        running.set(false);
        if (encryptorThread != null && encryptorThread.isAlive()) encryptorThread.interrupt();
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
    public EncryptionManager(String transformation) throws NoSuchPaddingException, NoSuchAlgorithmException {

        this.cipher = Cipher.getInstance(transformation);
        this.transformation = transformation;
        this.running = new AtomicBoolean(false);
        this.encryptorThread = null;
        this.observers = new ConcurrentLinkedQueue<>();
    }
}
