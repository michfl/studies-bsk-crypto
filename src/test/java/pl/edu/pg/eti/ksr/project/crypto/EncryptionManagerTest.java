package pl.edu.pg.eti.ksr.project.crypto;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class EncryptionManagerTest {

    private EncryptionManager manager;
    private Transformation transformation = Transformation.AES_CBC_PKCS5Padding;
    private final Path sourceFile = Path.of("./src/test/resources/test.txt");
    private final Path targetEncryptedFile = Path.of("./src/test/resources/encryptedTest.txt");
    private final Path targetDecryptedFile = Path.of("./src/test/resources/decryptedTest.txt");

    @Before
    public void init() throws NoSuchPaddingException, NoSuchAlgorithmException {
        manager = new EncryptionManager(transformation.getText());
    }

    @After
    public void teardown() {
        File trgEncrypted = targetEncryptedFile.toFile();
        File trgDecrypted = targetDecryptedFile.toFile();
        if (trgEncrypted.exists()) trgEncrypted.delete();
        if (trgDecrypted.exists()) trgDecrypted.delete();
    }

    @Test
    public void Should_HaveNewTransformationSet_When_TransformationChanged()
            throws NoSuchPaddingException, NoSuchAlgorithmException {

        manager.setTransformation(Transformation.RSA_ECB_PKCS1Padding.getText());
        Assert.assertEquals(Transformation.RSA_ECB_PKCS1Padding.getText(), manager.getTransformation());
    }

    @Test
    public void Should_EncryptProvidedText_When_PerformingTextEncryption()
            throws NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException {

        String text = "Example text";

        Key key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        IvParameterSpec iv = EncryptionManager.generateIv(transformation.getBlockSize());

        String cipherText = new String(manager.encrypt(text, key, iv), StandardCharsets.UTF_8);
        Assert.assertNotEquals(text, cipherText);
    }

    @Test
    public void Should_ProperlyEncryptAndDecryptText_When_EncryptingWithDifferentTransformations()
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

        /*
        IMPORTANT

        CBC must use IV
        ECB cannot use IV

        AES has 16 block size
        DES and 3DES have 8 block size

        When no padding mode used, data size must be a multiplicity of block size.
         */

        String text = "Example text    "; // 16 size for testing
        Key key;
        KeyPair keyPair;
        IvParameterSpec iv;
        byte[] cipherText;

        // AES algorithm | CBC | No padding | 16 block size | using IV
        transformation = Transformation.AES_CBC_NoPadding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        iv = EncryptionManager.generateIv(transformation.getBlockSize());
        cipherText = manager.encrypt(text, key, iv);
        String decipheredTextAESCBCNoPadding = manager.decrypt(cipherText, key, iv);

        // AES algorithm | CBC | PKCS5 Padding | 16 block size | using IV
        transformation = Transformation.AES_CBC_PKCS5Padding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        iv = EncryptionManager.generateIv(transformation.getBlockSize());
        cipherText = manager.encrypt(text, key, iv);
        String decipheredTextAESCBCPKCS5Padding = manager.decrypt(cipherText, key, iv);

        // AES algorithm | ECB | No padding | 16 block size | no IV
        transformation = Transformation.AES_ECB_NoPadding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        cipherText = manager.encrypt(text, key);
        String decipheredTextAESECBNoPadding = manager.decrypt(cipherText, key);

        // AES algorithm | ECB | PKCS5 Padding | 16 block size | no IV
        transformation = Transformation.AES_ECB_PKCS5Padding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        cipherText = manager.encrypt(text, key);
        String decipheredTextAESECBPKCS5Padding = manager.decrypt(cipherText, key);


        // DES algorithm | CBC | No padding | 8 block size | using IV
        transformation = Transformation.DES_CBC_NoPadding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        iv = EncryptionManager.generateIv(transformation.getBlockSize());
        cipherText = manager.encrypt(text, key, iv);
        String decipheredTextDESCBCNoPadding = manager.decrypt(cipherText, key, iv);

        // DES algorithm | CBC | PKCS5 Padding | 8 block size | using IV
        transformation = Transformation.DES_CBC_PKCS5Padding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        iv = EncryptionManager.generateIv(transformation.getBlockSize());
        cipherText = manager.encrypt(text, key, iv);
        String decipheredTextDESCBCPKCS5Padding = manager.decrypt(cipherText, key, iv);

        // DES algorithm | ECB | No padding | 8 block size | no IV
        transformation = Transformation.DES_ECB_NoPadding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        cipherText = manager.encrypt(text, key);
        String decipheredTextDESECBNoPadding = manager.decrypt(cipherText, key);

        // DES algorithm | ECB | PKCS5 Padding | 8 block size | no IV
        transformation = Transformation.DES_ECB_PKCS5Padding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        cipherText = manager.encrypt(text, key);
        String decipheredTextDESECBPKCS5Padding = manager.decrypt(cipherText, key);


        // DESede algorithm | CBC | No padding | 8 block size | using IV
        transformation = Transformation.DESede_CBC_NoPadding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        iv = EncryptionManager.generateIv(transformation.getBlockSize());
        cipherText = manager.encrypt(text, key, iv);
        String decipheredTextDESedeCBCNoPadding = manager.decrypt(cipherText, key, iv);

        // DESede algorithm | CBC | PKCS5 Padding | 8 block size | using IV
        transformation = Transformation.DESede_CBC_PKCS5Padding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        iv = EncryptionManager.generateIv(transformation.getBlockSize());
        cipherText = manager.encrypt(text, key, iv);
        String decipheredTextDESedeCBCPKCS5Padding = manager.decrypt(cipherText, key, iv);

        // DESede algorithm | ECB | No padding | 8 block size | no IV
        transformation = Transformation.DESede_ECB_NoPadding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        cipherText = manager.encrypt(text, key);
        String decipheredTextDESedeECBNoPadding = manager.decrypt(cipherText, key);

        // DESede algorithm | ECB | PKCS5 Padding | 8 block size | no IV
        transformation = Transformation.DESede_ECB_PKCS5Padding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        cipherText = manager.encrypt(text, key);
        String decipheredTextDESedeECBPKCS5Padding = manager.decrypt(cipherText, key);


        // RSA / DSA algorithm | ECB | PKCS1 Padding | no IV | public and private keys
        transformation = Transformation.RSA_ECB_PKCS1Padding;
        manager.setTransformation(transformation.getText());
        keyPair = EncryptionManager.generateKeyPair(transformation.getKeySize(),
                transformation.getAlgorithm());
        cipherText = manager.encrypt(text, keyPair.getPublic());
        String decipheredTextRSAECBPKCS1Padding = manager.decrypt(cipherText, keyPair.getPrivate());

        // RSA / DSA algorithm | ECB | 1AndMGF1 Padding | no IV | public and private keys
        transformation = Transformation.RSA_ECB_OAEPWithSHA_1AndMGF1Padding;
        manager.setTransformation(transformation.getText());
        keyPair = EncryptionManager.generateKeyPair(transformation.getKeySize(),
                transformation.getAlgorithm());
        cipherText = manager.encrypt(text, keyPair.getPublic());
        String decipheredTextRSAECB1AndMGF1Padding = manager.decrypt(cipherText, keyPair.getPrivate());

        // RSA / DSA algorithm | ECB | 256AndMGF1 Padding | no IV | public and private keys
        transformation = Transformation.RSA_ECB_OAEPWithSHA_256AndMGF1Padding;
        manager.setTransformation(transformation.getText());
        keyPair = EncryptionManager.generateKeyPair(transformation.getKeySize(),
                transformation.getAlgorithm());
        cipherText = manager.encrypt(text, keyPair.getPublic());
        String decipheredTextRSAECB256AndMGF1Padding = manager.decrypt(cipherText, keyPair.getPrivate());

        Assert.assertEquals(text, decipheredTextAESCBCNoPadding);
        Assert.assertEquals(text, decipheredTextAESCBCPKCS5Padding);
        Assert.assertEquals(text, decipheredTextAESECBNoPadding);
        Assert.assertEquals(text, decipheredTextAESECBPKCS5Padding);

        Assert.assertEquals(text, decipheredTextDESCBCNoPadding);
        Assert.assertEquals(text, decipheredTextDESCBCPKCS5Padding);
        Assert.assertEquals(text, decipheredTextDESECBNoPadding);
        Assert.assertEquals(text, decipheredTextDESECBPKCS5Padding);

        Assert.assertEquals(text, decipheredTextDESedeCBCNoPadding);
        Assert.assertEquals(text, decipheredTextDESedeCBCPKCS5Padding);
        Assert.assertEquals(text, decipheredTextDESedeECBNoPadding);
        Assert.assertEquals(text, decipheredTextDESedeECBPKCS5Padding);

        Assert.assertEquals(text, decipheredTextRSAECBPKCS1Padding);
        Assert.assertEquals(text, decipheredTextRSAECB1AndMGF1Padding);
        Assert.assertEquals(text, decipheredTextRSAECB256AndMGF1Padding);
    }

    @Test
    public void Should_CreateEncryptedFileOnTheSpecifiedPath_When_EncryptingToFile()
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            InterruptedException, IOException {

        Key key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        IvParameterSpec iv = EncryptionManager.generateIv(transformation.getBlockSize());

        manager.encrypt(sourceFile, targetEncryptedFile, key, iv, Files.size(sourceFile));
        manager.getEncryptorThread().join();

        Assert.assertTrue(targetEncryptedFile.toFile().exists());
    }

    @Test
    public void Should_SourceFileAndDecryptedSourceFileBeIdentical_When_PerformingFileEncryptionAndDecryptionUsingFileToFileMethod()
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            InterruptedException, IOException {

        Key key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        IvParameterSpec iv = EncryptionManager.generateIv(transformation.getBlockSize());

        manager.encrypt(sourceFile, targetEncryptedFile, key, iv, Files.size(sourceFile));
        manager.getEncryptorThread().join();

        manager.decrypt(targetEncryptedFile, targetDecryptedFile, key, iv, Files.size(sourceFile));
        manager.getEncryptorThread().join();

        long result = Files.mismatch(sourceFile, targetDecryptedFile);
        Assert.assertEquals(-1L, result);
    }

    @Test
    public void Should_AddCipheredDataToQueue_When_PerformingFileEncryptionToQueue()
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            InterruptedException, IOException {

        BlockingQueue<byte[]> blockingQueue = new LinkedBlockingDeque<>(1024);

        Key key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        IvParameterSpec iv = EncryptionManager.generateIv(transformation.getBlockSize());

        manager.encrypt(sourceFile, blockingQueue, key, iv, Files.size(sourceFile));
        manager.getEncryptorThread().join();

        Assert.assertFalse(blockingQueue.isEmpty());
    }

    @Test
    public void Should_SourceFileAndDecryptedSourceFileBeIdentical_When_PerformingFileEncryptionAndDecryptionUsingBlockingQueueMethod()
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            InterruptedException, IOException {
        BlockingQueue<byte[]> blockingQueue = new LinkedBlockingDeque<>(1024);

        Key key = EncryptionManager.generateKey(transformation.getKeySize(), transformation.getAlgorithm());
        IvParameterSpec iv = EncryptionManager.generateIv(transformation.getBlockSize());

        manager.encrypt(sourceFile, blockingQueue, key, iv, Files.size(sourceFile));
        manager.getEncryptorThread().join();

        manager.decrypt(blockingQueue, targetDecryptedFile, key, iv, Files.size(sourceFile));
        manager.getEncryptorThread().join();

        Assert.assertTrue(blockingQueue.isEmpty());
        long result = Files.mismatch(sourceFile, targetDecryptedFile);
        Assert.assertEquals(-1L, result);
    }

    @Test
    public void Should_DecryptedKeyBeIdenticalToOriginal_When_PerformingKeyEncryptionAndDecryption()
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException {

        Key key = EncryptionManager.generateKey(Transformation.AES_CBC_PKCS5Padding.getKeySize(),
                Transformation.AES_CBC_PKCS5Padding.getAlgorithm());

        transformation = Transformation.RSA_ECB_PKCS1Padding;
        manager.setTransformation(transformation.getText());
        KeyPair keyPair = EncryptionManager.generateKeyPair(transformation.getKeySize(),
                transformation.getAlgorithm());

        byte[] encryptedKey = manager.encrypt(key, keyPair.getPublic());
        Key decryptedKey = manager.decrypt(encryptedKey, keyPair.getPrivate(),
                Transformation.AES_CBC_PKCS5Padding.getAlgorithm());

        Assert.assertEquals(key, decryptedKey);
    }
}
