package com.sequenceiq.cloudbreak.websocket.message;

public class StackStatusMessage {

    private Long id;
    private String status;

    public StackStatusMessage(Long id, String status) {
        super();
        this.id = id;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
