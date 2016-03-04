package io.coriolis.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.server.ServerFactory;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ApiConfiguration extends Configuration {

    @NotEmpty
    @JsonProperty
    private String defaultName = "Coriolis API";

    @NotEmpty
    @JsonProperty
    private String eddnHost;

    @JsonProperty
    private int eddnPort;

    @NotEmpty
    @JsonProperty
    private String eddbSystemJSONUrl;

    @NotEmpty
    @JsonProperty
    private String eddbStationJSONUrl;

    @NotEmpty
    @JsonProperty
    private String adminUser;

    @NotEmpty
    @JsonProperty
    private String adminPassword;

    @Valid
    @NotNull
    @JsonProperty
    private HttpClientConfiguration httpClientConfiguration = new HttpClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private HttpClientConfiguration companionClientConfiguration = new HttpClientConfiguration();

    public HttpClientConfiguration getHttpClientConfiguration() {
        return httpClientConfiguration;
    }

    public HttpClientConfiguration getCompanionClientConfiguration() {
        return companionClientConfiguration;
    }

    public String getDefaultName() {
        return defaultName;
    }

    public String getEddnHost() {
        return eddnHost;
    }

    public int getEddnPort() {
        return eddnPort;
    }

    public String getEddbSystemJSONUrl() {
        return eddbSystemJSONUrl;
    }

    public String getEddbStationJSONUrl() {
        return eddbStationJSONUrl;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public String getAdminUser() {
        return adminUser;
    }

}
