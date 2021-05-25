package com.sequenceiq.cdp.databus.model;

import java.io.Serializable;

import com.cloudera.cdp.shaded.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.cloudera.cdp.shaded.com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordReply implements Serializable {

    private String recordId;

    private RecordStatus status;

    private String uploadUrl;

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    @Override
    public String toString() {
        return "RecordReply{" +
                "recordId='" + recordId + '\'' +
                ", status=" + status +
                ", uploadUrl='" + uploadUrl + '\'' +
                '}';
    }
}
