package pl.edu.pg.eti.ksr.project.crypto;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class EncryptionManagerTest {

    private EncryptionManager manager;
    private Transformation transformation = Transformation.AES_CBC_PKCS5Padding;

    @Before
    public void init() throws NoSuchPaddingException, NoSuchAlgorithmException {
        manager = new EncryptionManager(transformation.getText());
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

        Key key = EncryptionManager.generateKey(transformation.getKeySizes()[0], transformation.getAlgorithm());
        IvParameterSpec iv = EncryptionManager.generateIv(16);

        String cipherText = new String(manager.encrypt(text, key, iv), StandardCharsets.UTF_8);
        Assert.assertNotEquals(text, cipherText);
    }

    @Test
    public void Should_DecryptedTextBeTheSame_When_PerformingTextDecryption()
            throws NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException {

        String text = "Example text";

        Key key = EncryptionManager.generateKey(transformation.getKeySizes()[0], transformation.getAlgorithm());
        IvParameterSpec iv = EncryptionManager.generateIv(16);

        byte[] cipherText = manager.encrypt(text, key, iv);

        String decipheredText = manager.decrypt(cipherText, key, iv);

        Assert.assertEquals(text, decipheredText);
    }

    @Test
    public void Should_ProperlyEncryptAndDecryptText_When_EncryptingWithDifferentTransformations()
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

        String text = "Example text";
        Key key;
        IvParameterSpec iv;
        byte[] cipherText;

        transformation = Transformation.DES_CBC_PKCS5Padding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySizes()[0], transformation.getAlgorithm());
        iv = EncryptionManager.generateIv(8);
        cipherText = manager.encrypt(text, key, iv);
        String decipheredTextDES = manager.decrypt(cipherText, key, iv);

        transformation = Transformation.DESede_CBC_PKCS5Padding;
        manager.setTransformation(transformation.getText());
        key = EncryptionManager.generateKey(transformation.getKeySizes()[0], transformation.getAlgorithm());
        iv = EncryptionManager.generateIv(8);
        cipherText = manager.encrypt(text, key, iv);
        String decipheredTextDESede = manager.decrypt(cipherText, key, iv);

        Assert.assertEquals(text, decipheredTextDES);
        Assert.assertEquals(text, decipheredTextDESede);
    }
}
