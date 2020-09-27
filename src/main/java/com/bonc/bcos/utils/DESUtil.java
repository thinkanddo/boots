package com.bonc.bcos.utils;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

public class DESUtil{
    private static final String DES = "DESede";
    public static final String DES_KEY = "bonc_bcos_&n*2b_2u";

    public static byte[] encrypt(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2) throws RuntimeException {
        try{
            SecureRandom localSecureRandom = new SecureRandom();

            DESedeKeySpec localDESedeKeySpec = new DESedeKeySpec(paramArrayOfByte2);

            SecretKeyFactory localSecretKeyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey localSecretKey = localSecretKeyFactory.generateSecret(localDESedeKeySpec);

            Cipher localCipher = Cipher.getInstance("DESede");

            localCipher.init(1, localSecretKey, localSecureRandom);

            return localCipher.doFinal(paramArrayOfByte1);
        } catch (Exception localException) {
            throw new RuntimeException(localException);
        }
    }

    public static byte[] decrypt(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2) throws RuntimeException{
        try{
            SecureRandom localSecureRandom = new SecureRandom();

            DESedeKeySpec localDESedeKeySpec = new DESedeKeySpec(paramArrayOfByte2);

            SecretKeyFactory localSecretKeyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey localSecretKey = localSecretKeyFactory.generateSecret(localDESedeKeySpec);

            Cipher localCipher = Cipher.getInstance("DESede");

            localCipher.init(2, localSecretKey, localSecureRandom);

            return localCipher.doFinal(paramArrayOfByte1);
        } catch (Exception localException) {
            throw new RuntimeException(localException);
        }
    }

    public static String decrypt(String paramString){
        if (paramString == null) {
            return null;
        }
        return new String(decrypt(hex2byte(paramString.getBytes()),DES_KEY.getBytes()));
    }

    public static String decrypt(String data, String salt){
        if (StringUtils.isEmpty(data)) {
            return null;
        }
        return new String(decrypt(hex2byte(data.getBytes()), (DES_KEY+salt).getBytes()));
    }

    public static String encrypt(String paramString){
        if (paramString != null) {
            try {
                return byte2hex(encrypt(paramString.getBytes(), DES_KEY.getBytes()));
            } catch (Exception localException) {
                throw new RuntimeException(localException);
            }
        }
        return null;
    }

    public static String encrypt(String data, String salt){
        if (!StringUtils.isEmpty(data) && !StringUtils.isEmpty(salt)) {
            try {
                return byte2hex(encrypt(data.getBytes(), (DES_KEY+salt).getBytes()));
            } catch (Exception localException) {
                throw new RuntimeException(localException);
            }
        }
        return null;
    }

    private static String byte2hex(byte[] paramArrayOfByte){
        StringBuilder localStringBuilder = new StringBuilder();

        for (int i = 0; (paramArrayOfByte != null) && (i < paramArrayOfByte.length); i++) {
            String str = Integer.toHexString(paramArrayOfByte[i] & 0xFF);
            if (str.length() == 1) {
                localStringBuilder.append('0');
            }
            localStringBuilder.append(str);
        }
        return localStringBuilder.toString().toUpperCase();
    }

    private static byte[] hex2byte(byte[] paramArrayOfByte) {
        if (paramArrayOfByte.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        byte[] arrayOfByte = new byte[paramArrayOfByte.length / 2];
        for (int i = 0; i < paramArrayOfByte.length; i += 2) {
            String str = new String(paramArrayOfByte, i, 2);
            arrayOfByte[(i / 2)] = ((byte)Integer.parseInt(str, 16));
        }
        return arrayOfByte;
    }

    public static void main(String[] args){
        String action = "{\"code\":\"AE0001\",\"ips\":[\"172.16.11.134\",\"172.16.11.161\"]}";
        System.out.println(action);

        String ret  = encrypt(action,"AE0001");
        System.out.println(ret);

        action = decrypt(ret,"AE0001");
        System.out.println(action);
    }
}