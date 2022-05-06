package pl.edu.pg.eti.ksr.project.crypto;

import lombok.Getter;

/**
 * Supported Cipher transformations.
 * (key size in bits)
 */
public enum Transformation {

    AES_CBC_NoPadding("AES/CBC/NoPadding", new int[]{128}),
    AES_CBC_PKCS5Padding("AES/CBC/PKCS5Padding", new int[]{128}),
    AES_ECB_NoPadding("AES/ECB/NoPadding", new int[]{128}),
    AES_ECB_PKCS5Padding("AES/ECB/PKCS5Padding", new int[]{128}),
    DES_CBC_NoPadding("DES/CBC/NoPadding", new int[]{56}),
    DES_CBC_PKCS5Padding("DES/CBC/PKCS5Padding", new int[]{56}),
    DES_ECB_NoPadding("DES/ECB/NoPadding", new int[]{56}),
    DES_ECB_PKCS5Padding("DES/ECB/PKCS5Padding", new int[]{56}),
    DESede_CBC_NoPadding("DESede/CBC/NoPadding", new int[]{168}),
    DESede_CBC_PKCS5Padding("DESede/CBC/PKCS5Padding", new int[]{168}),
    DESede_ECB_NoPadding("DESede/ECB/NoPadding", new int[]{168}),
    DESede_ECB_PKCS5Padding("DESede/ECB/PKCS5Padding", new int[]{168}),
    RSA_ECB_PKCS1Padding("RSA/ECB/PKCS1Padding", new int[]{1024, 2048}),
    RSA_ECB_OAEPWithSHA_1AndMGF1Padding("RSA/ECB/OAEPWithSHA-1AndMGF1Padding", new int[]{1024, 2048}),
    RSA_ECB_OAEPWithSHA_256AndMGF1Padding("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", new int[]{1024, 2048});

    @Getter
    private final String text;

    @Getter
    private final int[] keySizes;

    public String getAlgorithm() {
        return text.split("/")[0];
    }

    Transformation(String text, int[] keySizes) {
        this.text = text;
        this.keySizes = keySizes;
    }
}
