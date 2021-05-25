package com.sequenceiq.cdp.databus.model;

import java.io.Serializable;
import java.util.List;

import com.cloudera.cdp.shaded.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.cloudera.cdp.shaded.com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Record implements Serializable {

    private String streamName;

    private String partitionKey;

    private String payload;

    private long payloadSize;

    private List<Header> headers;

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    public long getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(long payloadSize) {
        this.payloadSize = payloadSize;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "Record{" +
                "streamName='" + streamName + '\'' +
                ", partitionKey='" + partitionKey + '\'' +
                ", payload='" + payload + '\'' +
                ", payloadSize=" + payloadSize +
                ", headers=" + headers +
                '}';
    }
}
