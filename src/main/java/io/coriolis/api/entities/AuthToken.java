package io.coriolis.api.entities;

import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;


public class AuthToken {

    private DateTime expires;
    private long commanderId;
    private String companionAppCookie;
    private String companionMidCookie;
    private String companionMtkCookie;

    public AuthToken(long commanderId, String companionAppCookie, String companionMidCookie, String companionMtkCookie, DateTime expires) {
        this.commanderId = commanderId;
        this.companionAppCookie = companionAppCookie;
        this.companionMidCookie = companionMidCookie;
        this.companionMtkCookie = companionMtkCookie;
        this.expires = expires;
    }

    public DateTime getExpires() {
        return expires;
    }

    public boolean isExpired() {
        return expires.isBeforeNow();
    }

    public long getCommanderId() {
        return commanderId;
    }

    public String getCompanionAppCookie() {
        return companionAppCookie;
    }

    public String getCompanionMidCookie() {
        return companionMidCookie;
    }

    public String getCompanionMtkCookie() {
        return companionMtkCookie;
    }

    public void updateCompanionCookies(String app, String mid, String mtk, DateTime expires) {
        this.companionAppCookie = app;
        this.companionMidCookie = mid;
        this.companionMtkCookie = mtk;
        this.expires = expires;
    }

    public static AuthToken decrypt(String token, SecretKeySpec key) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(Base64.decodeBase64(token.substring(0,31)));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        String parts[] =  new String(cipher.doFinal(Base64.decodeBase64(token.substring(32)))).split(";");

        return new AuthToken(Long.parseLong(parts[0]), parts[1], parts[2], parts[3], new DateTime(parts[4]));
    }

    public Map<String, Object> encrypt(SecretKeySpec key) throws Exception {
        Map<String, Object> data = new HashMap<>();
        SecureRandom random = new SecureRandom();
        byte[] ivBytes = new byte[32];

        random.nextBytes(ivBytes);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        String blob = commanderId + ";"
                + companionAppCookie + ";"
                + companionMidCookie + ";"
                + companionMtkCookie + ";"
                + expires;

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        data.put("expires", expires);
        data.put("token",  Base64.encodeBase64String(ivBytes) + Base64.encodeBase64String(cipher.doFinal(blob.getBytes())));

        return data;
    }

}
