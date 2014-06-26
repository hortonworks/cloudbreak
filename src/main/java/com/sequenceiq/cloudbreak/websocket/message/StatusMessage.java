package com.sequenceiq.cloudbreak.websocket.message;

public class StatusMessage {

    private Long id;
    private String name;
    private String status;
    private String detailedMessage;

    public StatusMessage(Long id, String name, String status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    public StatusMessage(Long id, String name, String status, String detailedMessage) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.detailedMessage = detailedMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public void setDetailedMessage(String detailedMessage) {
        this.detailedMessage = detailedMessage;
    }

    @Override
    public String toString() {
        return "StatusMessage [id=" + id + ", name=" + name + ", status=" + status + ", detailedMessage=" + detailedMessage + "]";
    }

}
