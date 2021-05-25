package com.sequenceiq.cdp.databus.model;

import java.io.Serializable;

import com.cloudera.cdp.shaded.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.cloudera.cdp.shaded.com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PutRecordResponse extends CdpResponse implements Serializable {

    private RecordReply record;

    public RecordReply getRecord() {
        return record;
    }

    public void setRecord(RecordReply record) {
        this.record = record;
    }

    @Override
    public String toString() {
        return "PutRecordResponse{" +
                "record=" + record +
                "} " + super.toString();
    }
}
