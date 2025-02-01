package com.zura.ar_rules.encryption;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import org.springframework.stereotype.Service;

/**
 *
 * @author Rehman
 */
@Service("Base64Generator")
public class Base64Generator {

    public String decryptToString(String encryptString){
        Decoder decoder = Base64.getDecoder();
        byte[] decodedByte = decoder.decode(encryptString.replaceAll("J1p20IBs23",""));
        String decodedString = new String(decodedByte);
        //System.out.println(decodedString);  // Outputs: "Highlight"
        return decodedString;
    }


    public String stringToEncrypt(String rawString){
        Encoder encoder = Base64.getEncoder();
        String encodedString = encoder.encodeToString(rawString.getBytes());
        //System.out.println(encodedString); // Outputs: "SGlnaGxpZ2h0
        String finalString = "";
        for(int index=0;index<encodedString.length();index++){

            if(index == 2 || index == Math.round(encodedString.length()/2)){
                finalString += "J1p20IBs23";
            }
            finalString += encodedString.charAt(index);
        }

        return encodedString;
    }

    public String aToBEncrypt(String rawString){
        String encoded = new String(Base64.getEncoder().encode(rawString.getBytes()));
        return encoded;
    }
}
