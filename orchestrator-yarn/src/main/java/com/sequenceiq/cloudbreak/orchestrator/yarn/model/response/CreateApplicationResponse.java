package com.sequenceiq.cloudbreak.orchestrator.yarn.model.response;

public class CreateApplicationResponse  implements ApplicationResponse  {

    private String uri;

    private String state;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
