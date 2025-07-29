package com.sequenceiq.cloudbreak.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RpcBatchResult<R> {
    private String error;

    @JsonProperty("error_code")
    private Integer errorCode;

    @JsonProperty("error_name")
    private String errorName;

    @JsonProperty("error_kw")
    private RpcBatchErrorKw errorKw;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<R> result;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorName() {
        return errorName;
    }

    public void setErrorName(String errorName) {
        this.errorName = errorName;
    }

    public RpcBatchErrorKw getErrorKw() {
        return errorKw;
    }

    public void setErrorKw(RpcBatchErrorKw errorKw) {
        this.errorKw = errorKw;
    }

    public List<R> getResult() {
        return result;
    }

    public void setResult(List<R> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "RpcBatchResult{" +
                "error='" + error + '\'' +
                ", errorCode=" + errorCode +
                ", errorName='" + errorName + '\'' +
                ", errorKw=" + errorKw +
                ", result=" + result +
                '}';
    }
}
