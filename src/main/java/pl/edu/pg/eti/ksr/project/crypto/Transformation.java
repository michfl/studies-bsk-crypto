package pl.edu.pg.eti.ksr.project.crypto;

import lombok.Getter;

/**
 * Supported Cipher transformations.
 * (key size in bits)
 *
 * In RSA block size is equal to key size.
 */
public enum Transformation {

    AES_CBC_NoPadding("AES/CBC/NoPadding", 16, 128),
    AES_CBC_PKCS5Padding("AES/CBC/PKCS5Padding", 16, 128),
    AES_ECB_NoPadding("AES/ECB/NoPadding", 16, 128),
    AES_ECB_PKCS5Padding("AES/ECB/PKCS5Padding", 16, 128),
    DES_CBC_NoPadding("DES/CBC/NoPadding", 8, 56),
    DES_CBC_PKCS5Padding("DES/CBC/PKCS5Padding", 8, 56),
    DES_ECB_NoPadding("DES/ECB/NoPadding", 8, 56),
    DES_ECB_PKCS5Padding("DES/ECB/PKCS5Padding", 8, 56),
    DESede_CBC_NoPadding("DESede/CBC/NoPadding", 8, 168),
    DESede_CBC_PKCS5Padding("DESede/CBC/PKCS5Padding", 8, 168),
    DESede_ECB_NoPadding("DESede/ECB/NoPadding", 8, 168),
    DESede_ECB_PKCS5Padding("DESede/ECB/PKCS5Padding", 8, 168),
    RSA_ECB_PKCS1Padding("RSA/ECB/PKCS1Padding", 0, 2048),
    RSA_ECB_OAEPWithSHA_1AndMGF1Padding("RSA/ECB/OAEPWithSHA-1AndMGF1Padding", 0, 2048),
    RSA_ECB_OAEPWithSHA_256AndMGF1Padding("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", 0, 2048);

    @Getter
    private final String text;

    @Getter
    private final int blockSize;

    @Getter
    private final int keySize;

    public String getAlgorithm() {
        return text.split("/")[0];
    }

    public String getMode() {
        return text.split("/")[1];
    }

    public String getPadding() {
        return text.split("/")[2];
    }

    Transformation(String text, int blockSize, int keySize) {
        this.text = text;
        this.blockSize = blockSize;
        this.keySize = keySize;
    }
}
