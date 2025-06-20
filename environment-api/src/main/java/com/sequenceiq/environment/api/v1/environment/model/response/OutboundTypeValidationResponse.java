package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.type.OutboundType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutboundTypeValidationResponse {

    private Map<String, OutboundType> stackOutboundTypeMap;

    private OutboundType ipaOutboundType;

    private String message;

    public OutboundTypeValidationResponse() {
    }

    public Map<String, OutboundType> getStackOutboundTypeMap() {
        return stackOutboundTypeMap;
    }

    public void setStackOutboundTypeMap(Map<String, OutboundType> stackOutboundTypeMap) {
        this.stackOutboundTypeMap = stackOutboundTypeMap;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OutboundType getIpaOutboundType() {
        return ipaOutboundType;
    }

    public void setIpaOutboundType(OutboundType ipaOutboundType) {
        this.ipaOutboundType = ipaOutboundType;
    }

    @Override
    public String toString() {
        return "OutboundTypeValidationResponse{" +
                "stackOutboundTypeMap=" + stackOutboundTypeMap +
                ", ipaOutboundType=" + ipaOutboundType +
                ", message='" + message + '\'' +
                '}';
    }
}
