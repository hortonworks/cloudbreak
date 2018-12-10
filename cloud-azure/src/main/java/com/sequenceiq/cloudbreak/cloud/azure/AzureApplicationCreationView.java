package com.sequenceiq.cloudbreak.cloud.azure;

public class AzureApplicationCreationView {

    private String appIdentifierURI;

    private String appSecret;

    private String replyURL;

    private String appCreationRequestPayload;

    public AzureApplicationCreationView(String appIdentifierURI, String appSecret, String replyURL, String appCreationRequestPayload) {
        this.appIdentifierURI = appIdentifierURI;
        this.appSecret = appSecret;
        this.replyURL = replyURL;
        this.appCreationRequestPayload = appCreationRequestPayload;
    }

    public String getAppIdentifierURI() {
        return appIdentifierURI;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getReplyURL() {
        return replyURL;
    }

    public String getAppCreationRequestPayload() {
        return appCreationRequestPayload;
    }
}
