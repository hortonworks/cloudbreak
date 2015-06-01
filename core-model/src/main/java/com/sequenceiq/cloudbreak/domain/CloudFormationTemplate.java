package com.sequenceiq.cloudbreak.domain;

public class CloudFormationTemplate {

    private String body;

    public CloudFormationTemplate(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}
