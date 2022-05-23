package pl.edu.pg.eti.ksr.project.accounts;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AccountManager {

    private static Map<String, Integer> users = new HashMap<>();
    private static String mainFilepath = "./BSK_files";

    public static Map<String, Integer> getUsers() {
        return users;
    }

    public static void populateAccounts() {
        users.put("test", "test".hashCode());
    }

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

            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(mainFilepath + "/private/" + username + "_private.txt", true));
            String encryptedPriv = encrypt((Base64.getEncoder().encodeToString(privateKey.getEncoded())), passHash);
            writer.append(encryptedPriv);
            writer.append('\n');
            writer.close();

            BufferedWriter writer1 = new BufferedWriter(
                    new FileWriter(mainFilepath + "/public/" + username + "_public.txt", true));
            String encryptedPub = encrypt((Base64.getEncoder().encodeToString(publicKey.getEncoded())), passHash);
            writer1.append(encryptedPub);
            writer1.append('\n');
            writer1.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String encrypt(String toEncrypt, Integer key) {
        try {
            //Generate key and iv
            IvParameterSpec ivParams = generateIv();
            SecretKey secretKey = getKeyFromPassword(key, 128, "12345678");

            //Init cypher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);

            //Encrypt
            byte[] encrypted = cipher.doFinal(toEncrypt.getBytes());
            return Base64.getEncoder().encodeToString(ivParams.getIV()) + " " + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String decrypt(String toDecrypt, Integer key) {
        String[] ivAndMess = getIvAndMessage(toDecrypt);
        try {
            //Get key and iv
            IvParameterSpec ivParams = new IvParameterSpec(Base64.getDecoder().decode(ivAndMess[0]));
            SecretKey secretKey = getKeyFromPassword(key, 128, "12345678");

            //Init cypher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);

            //Decrypt
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(ivAndMess[1]));

            return new String(original);
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

    private static String[] getIvAndMessage(String k) {
        return k.split(" ", 2);
    }
}
