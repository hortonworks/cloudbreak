package com.sequenceiq.cloudbreak.client;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RPCResponse<R> {
    private Integer count;

    private List<RPCMessage> messages;

    private R result;

    private Object failed;

    private Object completed;

    private Object value;

    private String summary;

    private Boolean truncated;

    private List<RpcBatchResult<R>> results;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<RPCMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<RPCMessage> messages) {
        this.messages = messages;
    }

    public R getResult() {
        return result;
    }

    public void setResult(R result) {
        this.result = result;
    }

    public Object getFailed() {
        return failed;
    }

    public void setFailed(Object failed) {
        this.failed = failed;
    }

    public Object getCompleted() {
        return completed;
    }

    public void setCompleted(Object completed) {
        this.completed = completed;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Boolean getTruncated() {
        return truncated;
    }

    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }

    public List<RpcBatchResult<R>> getResults() {
        return results;
    }

    public void setResults(List<RpcBatchResult<R>> results) {
        this.results = results;
    }

    @JsonIgnore
    public RPCMessage getFirstRpcMessage() {
        return CollectionUtils.isNotEmpty(messages) ? messages.get(0) : null;
    }

    @JsonIgnore
    public String getFirstTextMessage() {
        return getFirstRpcMessage() != null ? getFirstRpcMessage().getMessage() : null;
    }

    @Override
    public String toString() {
        return "RPCResponse{" +
                "count=" + count +
                ", messages=" + messages +
                ", result=" + result +
                ", failed=" + failed +
                ", completed=" + completed +
                ", value=" + value +
                ", summary='" + summary + '\'' +
                ", truncated=" + truncated +
                ", results=" + results +
                '}';
    }
}
