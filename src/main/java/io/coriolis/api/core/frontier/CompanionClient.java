package io.coriolis.api.core.frontier;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.coriolis.api.entities.CompanionSession;
import io.coriolis.api.resources.exceptions.JsonWebApplicationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class CompanionClient {

    final static Logger logger = LoggerFactory.getLogger(CompanionClient.class);

    private static final String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 8_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12B411";
    private static final String companionHost = "https://companion.orerve.net";

    private HttpClient httpClient;

    public CompanionClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }


    public CompanionSession login(String email, String password) {
        CookieStore cookieStore = new BasicCookieStore();
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("email", email));
        urlParameters.add(new BasicNameValuePair("password", password));

        HttpPost postRequest = new HttpPost(companionHost + "/user/login");

        try {
            postRequest.setEntity(new UrlEncodedFormEntity(urlParameters));
        } catch (UnsupportedEncodingException e) {
            logger.error("Unable to encode email/password: " + e.getMessage());
            throw new JsonWebApplicationException("Unable to encode email/password", Response.Status.BAD_REQUEST);
        }

        HttpResponse response;

        try {
            response = executeRequest(postRequest, cookieStore);
        } catch (IOException e) {
            logger.error("Companion API Request failed: " + e.getMessage());
            throw new JsonWebApplicationException("Companion API Request failed", Response.Status.SERVICE_UNAVAILABLE);
        }

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_TEMPORARY_REDIRECT && response.getHeaders("Location").equals("/user/confirm")) {
            CompanionSession session = new CompanionSession();

            // Add cookies to companion session

            return session;
        }
        throw new JsonWebApplicationException("Login failed: Invalid Email/password", Response.Status.UNAUTHORIZED);
    }

    public CompanionSession confirm(CompanionSession session) {
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("code", session.getConfirmationCode()));

        HttpPost postRequest = new HttpPost(companionHost + "/user/confirm");
        try {
            postRequest.setEntity(new UrlEncodedFormEntity(urlParameters));
        } catch (UnsupportedEncodingException e) {
            logger.error("Unable to encode confirmation code: " + e.getMessage());
            throw new JsonWebApplicationException("Unable to encode confirmation code", Response.Status.BAD_REQUEST);
        }

        try {
            HttpResponse response = executeRequest(postRequest, session.getCookies());

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_TEMPORARY_REDIRECT && response.getHeaders("Location").equals("/")) {

                // Update session from cookie store

                return session;
            }
            throw new JsonWebApplicationException("Login failed: Invalid code", Response.Status.UNAUTHORIZED);

        } catch (IOException e) {
            logger.error("Companion API Request failed: " + e.getMessage());
            throw new JsonWebApplicationException("Companion API Request failed", Response.Status.SERVICE_UNAVAILABLE);
        }
    }

    public HttpEntity getProfile(CompanionSession session) {
        try {
            return executeRequest(new HttpGet(companionHost + "/profile"), session.getCookies()).getEntity();
        } catch (IOException e) {
            logger.error("Companion API Request failed: " + e.getMessage());
            throw new JsonWebApplicationException("Companion API Request failed", Response.Status.SERVICE_UNAVAILABLE);
        }
    }

    private HttpResponse executeRequest(HttpUriRequest request, CookieStore cookieStore) throws IOException {
        HttpContext localContext = new BasicHttpContext();
        localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
        request.setHeader("User-Agent", userAgent);

        return httpClient.execute(request, localContext);
    }

}
