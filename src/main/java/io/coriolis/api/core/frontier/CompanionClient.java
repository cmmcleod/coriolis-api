package io.coriolis.api.core.frontier;

import io.coriolis.api.resources.exceptions.JsonWebApplicationException;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class CompanionClient {

    final static Logger logger = LoggerFactory.getLogger(CompanionClient.class);
    public static final String COMPANION_HOSTNAME = "companion.orerve.net";

    HttpHost proxy = new HttpHost("127.0.0.1", 8888, "http");

    private HttpClient httpClient;
    private HttpHost companionHost;
    private RequestConfig config;

    public CompanionClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.config = RequestConfig.custom().setProxy(proxy).build();
        companionHost = new HttpHost(COMPANION_HOSTNAME, 443, "https");
    }


    public CookieStore login(String email, String password)  throws JsonWebApplicationException  {
        List <NameValuePair> credentials = new ArrayList<>();
        credentials.add(new BasicNameValuePair("email", email));
        credentials.add(new BasicNameValuePair("password", password));
        CookieStore cookies = new BasicCookieStore();

        HttpResponse response = post("/user/login", cookies, credentials);
        Header location = response.getFirstHeader("Location");
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 302 && location != null && location.getValue().equalsIgnoreCase("/user/confirm")) {
            return cookies;
        }
        throw new JsonWebApplicationException("Login failed: Invalid Email/password", Response.Status.UNAUTHORIZED);
    }

    public CookieStore confirm(String confirmationCode, CookieStore cookies) throws JsonWebApplicationException {
        List <NameValuePair> data = new ArrayList<>();
        data.add(new BasicNameValuePair("code", confirmationCode));
        HttpResponse response = post("/user/confirm", cookies, data);
        Header location = response.getFirstHeader("Location");
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 302 && location != null && location.getValue().equals("/")) {
            return cookies;
        } else if (statusCode == HttpStatus.SC_OK && location == null) {
            throw new JsonWebApplicationException("Confirmation failed: Invalid Code", Response.Status.UNAUTHORIZED);
        }
        throw new JsonWebApplicationException("Confirmation failed: Unknown", Response.Status.INTERNAL_SERVER_ERROR);
    }

    public InputStream getProfile(CookieStore cookies) throws JsonWebApplicationException, IOException {
        HttpResponse response = get("/profile", cookies);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return response.getEntity().getContent();
        }
        throw new JsonWebApplicationException("Session Timed out", Response.Status.UNAUTHORIZED);
    }


    private HttpResponse get(String path, CookieStore cookies) {
        HttpGet request = new HttpGet(path);
        request.setConfig(config);
        HttpContext context = new BasicHttpContext();

        if (cookies != null) {
            context.setAttribute(HttpClientContext.COOKIE_STORE, cookies);
        }

        try {
            return httpClient.execute(companionHost, request, context);
        } catch (IOException e) {
            logger.error("Companion API request failed: " + e.getMessage());
            throw new JsonWebApplicationException("Companion API request failed", Response.Status.SERVICE_UNAVAILABLE);
        }
    }

    private HttpResponse post(String path, CookieStore cookies, List<NameValuePair> data) {
        HttpPost request = new HttpPost(path);
        request.setConfig(config);
        HttpContext context = new BasicHttpContext();

        context.setAttribute(HttpClientContext.COOKIE_STORE, cookies);

        try {
            request.setEntity(new UrlEncodedFormEntity(data));
            return httpClient.execute(companionHost, request, context);
        } catch (UnsupportedEncodingException e) {
            logger.error("Companion API bad request: " + e.getMessage());
            throw new JsonWebApplicationException("Companion API bad request", Response.Status.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Companion API request failed: " + e.getMessage());
            e.printStackTrace();
            throw new JsonWebApplicationException("Companion API request failed", Response.Status.SERVICE_UNAVAILABLE);
        }
    }

}
