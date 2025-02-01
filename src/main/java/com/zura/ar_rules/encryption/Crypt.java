package com.zura.ar_rules.encryption;
import java.security.Key;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("Crypt")
public class Crypt {



    private static final String ALGO = "AES"; // Default uses ECB PKCS5Padding
    //private static final String SECRETKEY = "jasplabsmustbe16byteskey";

    //private static final String ENCODEBASE64KEY = encodeKey(SECRETKEY);

    public static String encrypt(String Data) throws Exception {

        String secretKey = KeyGenerator.generateAlphaNumericKey();
        int randomNum = Integer.valueOf(KeyGenerator.getRandomNumberFrom1to9());
        Key key = generateKey(encodeKey(secretKey));
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = c.doFinal(Data.getBytes());
        String encryptedValue = Base64.getEncoder().encodeToString(encVal);

        /*
        //System.out.println("encodedstring :::::");
        //System.out.println();
        //System.out.println();
        //System.out.println(encryptedValue);
        */

        String finalEncryptedString =  encryptedValue.substring(0,randomNum+1)+secretKey.toLowerCase()+encryptedValue.substring(randomNum+1,encryptedValue.length()-2)+randomNum+encryptedValue.substring(encryptedValue.length()-2);

        //return encryptedValue;
        return finalEncryptedString;
    }

    public static String decrypt(String strToDecrypt) {
        try {

            char getRandomNum = strToDecrypt.charAt(strToDecrypt.length()-3);
            int num = Character.getNumericValue(getRandomNum);
            String returnSecretKey = strToDecrypt.substring(num+1, (num+17));
            //System.out.println(returnSecretKey.toUpperCase());
            String getString = strToDecrypt.substring(0, (num+1))+strToDecrypt.substring(num+17, (strToDecrypt.length()-3))+strToDecrypt.substring(strToDecrypt.length()-2, strToDecrypt.length());

            /*
            //System.out.println("string to be decode :::::");
            //System.out.println();
            //System.out.println();
            //System.out.println(getString);
            */

            Key key = generateKey(encodeKey(returnSecretKey.toUpperCase()));
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(getString)));
            /*
            String getRandomNum = strToDecrypt.charAt(0)
            Key key = generateKey(ENCODEBASE64KEY);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
            */
        } catch (Exception e) {
            ////System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }


    private static Key generateKey(String secret) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(secret.getBytes());
        Key key = new SecretKeySpec(decoded, ALGO);
        return key;
    }

    public static String decodeKey(String str) {
        byte[] decoded = Base64.getDecoder().decode(str.getBytes());
        return new String(decoded);
    }

    public static String encodeKey(String str) {
        byte[] encoded = Base64.getEncoder().encode(str.getBytes());
        return new String(encoded);
    }

    public static void main(String a[]) throws Exception {
        /*
         * Secret Key must be in the form of 16 byte like,
         *
         * private static final byte[] secretKey = new byte[] { ‘m’, ‘u’, ‘s’, ‘t’, ‘b’,
         * ‘e’, ‘1’, ‘6’, ‘b’, ‘y’, ‘t’,’e’, ‘s’, ‘k’, ‘e’, ‘y’};
         *
         * below is the direct 16byte string we can use
         */

        ////System.out.println("EncodedBase64Key = " + encodedBase64Key); // This need to be share between client and server
        // To check actual key from encoded base 64 secretKey
        // String toDecodeBase64Key = decodeKey(encodedBase64Key);
        // //System.out.println("toDecodeBase64Key = "+toDecodeBase64Key);
        String toEncrypt = "Welcome to JASP LABS !!!";
        //System.out.println("Plain text = " + toEncrypt);

        // AES Encryption based on above secretKey
        //String encrStr = Crypt.encrypt(toEncrypt, encodedBase64Key);
        ////System.out.println("Cipher Text: Encryption of str = " + encrStr);
        // AES Decryption based on above secretKey

        String encrStr = "8jXlxvdpud0hmqaev7c6kuMf4mw9iGUa2ZQs+Q4==";
        String decrStr = Crypt.decrypt(encrStr);
        System.out.println("Decryption of str = " + decrStr);

    }
}
