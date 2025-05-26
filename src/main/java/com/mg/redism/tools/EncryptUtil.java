package com.mg.redism.tools;

import com.mg.redism.to.RConnection;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

/**
 * Created by mger on 02.09.2019.
 */
public class EncryptUtil {
    private char[] passKey;


    public EncryptUtil() {
        this(null);
    }

    public EncryptUtil(String passKeyString) {
        if (passKeyString == null) {
            passKeyString = "1G2aet62las@s3y7sdljcmn;wqe";
        }
        passKey = passKeyString.toCharArray();
    }

    private byte[] xor(byte[] openData) {
        //StringBuilder sb = new StringBuilder();
        byte[] ret = new byte[openData.length];

        for (int i = 0; i < openData.length; i++) {
            byte p = (byte) ((0xFF & openData[i]) ^ (0xFF & passKey[i % passKey.length]));
            //sb.append(p);
            ret[i] = p;
        }
        return ret;
    }


    public String encrypt(String openValue) {
        if (openValue == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(xor(openValue.getBytes()));
    }


    public String decrypt(String hidenValue) {
        if (hidenValue == null) {
            return null;
        }
        return new String(xor(Base64.getDecoder().decode(hidenValue)));
    }

    public Collection<RConnection> encryptRConnections(Collection<RConnection> rConnections) {
        List<RConnection> lst = new ArrayList<>();
        for (RConnection rConnection : rConnections) {
            RConnection rConnectionHidden = new RConnection();
            rConnectionHidden.setPass(encrypt(rConnection.getPass()));
            //rConnectionHidden.setPort(rConnection.getPort());
            rConnectionHidden.setServer(rConnection.getServer());
            rConnectionHidden.setName(rConnection.getName());
            rConnectionHidden.setSsl(rConnection.isSsl());
            lst.add(rConnectionHidden);
        }
        return lst;
    }

    public Collection<RConnection> decryptRConnections(Collection<RConnection> rConnections) {
        List<RConnection> lst = new ArrayList<>();
        for (RConnection rConnection : rConnections) {
            RConnection rConnectionOpen = new RConnection();
            rConnectionOpen.setPass(decrypt(rConnection.getPass()));
            //rConnectionOpen.setPort(rConnection.getPort());
            rConnectionOpen.setServer(rConnection.getServer());
            rConnectionOpen.setName(rConnection.getName());
            rConnectionOpen.setSsl(rConnection.isSsl());
            lst.add(rConnectionOpen);
        }
        return lst;
    }
//    public String decrypt(String hidenValue) {
//        StringBuilder sb = new StringBuilder();
//        char[] openData = hidenValue.toCharArray();
//        for (int i = 0; i < hidenValue.length(); i++) {
//            char p = (char) ((0xFF & openData[i]) ^ (0xFF & passKey[i % passKey.length]));
//            sb.append(p);
//        }
//        return sb.toString();
//    }

//    public static void main(String[] args) {
//        EncryptUtil encryptUtil = new EncryptUtil(null);
//        System.out.println(encryptUtil.encrypt("ABSZXIUYTREWQPOIUYTASDFGHJKL:ZXCVBNM<>?"));
//        System.out.println(encryptUtil.decrypt(encryptUtil.encrypt("1234567890-=+~!ABSZXIUYTREWQPOIUYTASDFGHJKL:ZXCVBNM<>?zxcvbnmasdfghjkl;'")));
//    }
}
