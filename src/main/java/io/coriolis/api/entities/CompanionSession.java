package io.coriolis.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Charsets;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.hibernate.validator.constraints.NotEmpty;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Date;


/**
 * Created by cmmcleod on 10/28/15.
 */
public class CompanionSession {

    @NotEmpty
    @JsonProperty
    private Date expires;

    @NotEmpty
    @JsonProperty
    private String iv;

    @JsonProperty
    private String confirmationCode;

    @JsonIgnore
    private CookieStore cookieStore;

    private Cipher cipher;
    private SecretKeySpec secretKey;

    public CompanionSession(Cipher cipher, SecretKeySpec secretKey) {
        this.cipher = cipher;
        this.secretKey = secretKey;
        iv = null;
        expires = null;
        cookieStore = new BasicCookieStore();
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public Date getExpires() {
        for (Cookie c : cookieStore.getCookies()) {
            if (expires == null || expires.after(c.getExpiryDate())) {
                expires = c.getExpiryDate();
            }
        }

        return expires;
    }

    public String getIv() {
        if (iv == null) {
            // generate new iv
        }

        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    @JsonProperty("session")
    public String getSession() {

        IvParameterSpec ivSpec = new IvParameterSpec(getIv().getBytes(Charsets.UTF_8));


        for (Cookie c : cookieStore.getCookies()) {
            c.
        }

        return "";
    }

    @JsonProperty("session")
    public void setSession(String session) throws InvalidAlgorithmParameterException, InvalidKeyException {
        IvParameterSpec ivSpec = new IvParameterSpec(getIv().getBytes(Charsets.UTF_8));
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        byte[] decrypted = new byte[cipher.getOutputSize(enc_len)];
        int dec_len = cipher.update(encrypted, 0, enc_len, decrypted, 0);
        dec_len += cipher.doFinal(decrypted, dec_len)

    }

    public CookieStore getCookies() {
        return cookieStore;
    }
}
