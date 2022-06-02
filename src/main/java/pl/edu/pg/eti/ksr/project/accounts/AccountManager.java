package pl.edu.pg.eti.ksr.project.accounts;

import lombok.Getter;
import lombok.Setter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

public class AccountManager {


    private static final String mainFilepath = "./BSK_files";

    @Getter
    @Setter
    private static Map<String, Integer> users = new HashMap<>();

    @Getter
    @Setter
    private static String username = null;

    @Getter
    @Setter
    private static Integer passHash = null;

    @Getter
    @Setter
    private static PrivateKey privKey = null;

    @Getter
    @Setter
    private static PublicKey publicKey = null;

    public static void initialize() {
        //Main folder
        File f = new File(mainFilepath);
        if (!f.exists()) {
            f.mkdir();
        }

        //Accounts file
        File fU = new File(mainFilepath + "/accounts.txt");
        if (!fU.exists()) {
            try {
                fU.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Private keys folder
        File fPr = new File(mainFilepath + "/private");
        if (!fPr.exists()) {
            fPr.mkdir();
        }

        //Public keys folder
        File fPu = new File(mainFilepath + "/public");
        if (!fPu.exists()) {
            fPu.mkdir();
        }
        readAccounts();
    }

    public static void readAccounts() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(mainFilepath + "/accounts.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                users.put(line, Integer.parseInt(reader.readLine()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addAccount(String username, Integer passHash) {
        //Add user credentials
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(mainFilepath + "/accounts.txt", true));
            writer.append(username);
            writer.append('\n');
            writer.append(passHash.toString());
            writer.append('\n');
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        users.put(username, passHash);

        //Generate keys
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();

            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            //Encrypt keys
            byte[] encPrivateKey = encryptBytes(new PKCS8EncodedKeySpec(privateKey.getEncoded()).getEncoded(), passHash);
            byte[] encPublicKey = encryptBytes(new X509EncodedKeySpec(publicKey.getEncoded()).getEncoded(), passHash);

            //Save encrypted private key
            try (FileOutputStream fos = new FileOutputStream(mainFilepath + "/private/" + username + "_encprivate.key")) {
                if (encPrivateKey != null) {
                    fos.write(encPrivateKey);
                }
            }

            //Save encrypted public key
            try (FileOutputStream fos = new FileOutputStream(mainFilepath + "/public/" + username + "_encpublic.key")) {
                if (encPublicKey != null) {
                    fos.write(encPublicKey);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] encryptBytes(byte[] bytes, Integer key) {
        //Generate key and IV
        IvParameterSpec ivParams = generateIv();
        SecretKey secretKey = getKeyFromPassword(key, 128, "12345678");

        //Encryption
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
            byte[] bytesEncrypted = cipher.doFinal(bytes);
            ByteBuffer byteBuffer = ByteBuffer.allocate(ivParams.getIV().length + bytesEncrypted.length);
            byteBuffer.put(ivParams.getIV());
            byteBuffer.put(bytesEncrypted);
            return byteBuffer.array();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] encryptFile(String filePath, Integer key) {
        File toEncrypt = new File(filePath);
        //Generate key and IV
        IvParameterSpec ivParams = generateIv();
        SecretKey secretKey = getKeyFromPassword(key, 128, "12345678");

        //Encryption
        try {
            byte[] bytesToEncrypt = Files.readAllBytes(toEncrypt.toPath());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
            byte[] bytesEncrypted = cipher.doFinal(bytesToEncrypt);
            ByteBuffer byteBuffer = ByteBuffer.allocate(ivParams.getIV().length + bytesEncrypted.length);
            byteBuffer.put(ivParams.getIV());
            byteBuffer.put(bytesEncrypted);
            return byteBuffer.array();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] decryptFile(String filePath, Integer key) {
        try {
            //Generate key and IV
            File toDecrypt = new File(filePath);
            byte[] bytesEncrypted = Files.readAllBytes(toDecrypt.toPath());
            IvParameterSpec ivParams = new IvParameterSpec(bytesEncrypted, 0, 16);
            SecretKey secretKey = getKeyFromPassword(key, 128, "12345678");

            //Init Cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
            return cipher.doFinal(bytesEncrypted, 16, bytesEncrypted.length - 16);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    private static SecretKey getKeyFromPassword(Integer passHash, int keySize, String salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(passHash.toString().toCharArray(), salt.getBytes(), 1000, keySize);
            SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            return secret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }
}
