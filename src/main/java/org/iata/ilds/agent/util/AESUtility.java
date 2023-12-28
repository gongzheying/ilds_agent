package org.iata.ilds.agent.util;

import lombok.extern.log4j.Log4j2;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;

@Log4j2
public final class AESUtility {

    private static final int IV_LENGTH = 16;

    private AESUtility(){
        //no op
    }

    public static String decrypt(String sSrc) {
        try {

            SecretKeySpec skeySpec = new SecretKeySpec(new byte[IV_LENGTH], "AES");


            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

            byte[] decoded_data = Base64.getDecoder().decode(sSrc);


            byte[] iv = Arrays.copyOfRange(decoded_data, 0, IV_LENGTH);

            IvParameterSpec ivParams = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivParams);

            byte[] encrypted1 = Arrays.copyOfRange(decoded_data, IV_LENGTH, decoded_data.length);


            byte[] original = cipher.doFinal(encrypted1);
            return new String(original);

        } catch (Exception ex) {
            log.error("Decrypt failed {}", ex.getMessage());
            return null;
        }
    }

    public static String encrypt(String sSrc) {
        try {

            SecretKeySpec skeySpec = new SecretKeySpec(new byte[IV_LENGTH], "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] data = cipher.doFinal(sSrc.getBytes());
            byte[] iv = cipher.getIV();

            byte[] encrypted = new byte[iv.length + data.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(data, 0, encrypted, iv.length, data.length);

            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception ex) {
            log.error("Encrypt failed: {}", ex.getMessage());
            return null;
        }
    }


}