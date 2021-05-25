package com.sequenceiq.cdp.databus.model;

import java.io.Serializable;

import com.cloudera.cdp.shaded.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.cloudera.cdp.shaded.com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PutRecordRequest implements Serializable {

    private Record record;

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    @Override
    public String toString() {
        return "PutRecordRequest{" +
                "record=" + record +
                '}';
    }
}
