package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.api.type.OutboundType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StackOutboundTypeValidationV4Response {

    @Schema(description = ModelDescriptions.UpgradeOutboundTypeDescription.CURRENT_OUTBOUND_TYPES, requiredMode = REQUIRED)
    private Map<String, OutboundType> stackOutboundTypeMap;

    @Schema(description = ModelDescriptions.UpgradeOutboundTypeDescription.OUTBOUND_UPGRADE_VALIDATION_MESSAGE)
    private String message;

    public StackOutboundTypeValidationV4Response() {
    }

    public StackOutboundTypeValidationV4Response(Map<String, OutboundType> stackOutboundTypeMap) {
        this.stackOutboundTypeMap = stackOutboundTypeMap;
        this.message = constructMessage(stackOutboundTypeMap);
    }

    public Map<String, OutboundType> getStackOutboundTypeMap() {
        return stackOutboundTypeMap;
    }

    public void setStackOutboundTypeMap(Map<String, OutboundType> stackOutboundTypeMap) {
        this.stackOutboundTypeMap = stackOutboundTypeMap;
        this.message = constructMessage(stackOutboundTypeMap);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private String constructMessage(Map<String, OutboundType> stackOutboundTypeMap) {
        if (stackOutboundTypeMap == null || stackOutboundTypeMap.isEmpty() || stackOutboundTypeMap.values().stream().noneMatch(OutboundType::isUpgradeable)) {
            return "No stacks found.";
        }
        StringBuilder messageBuilder = new StringBuilder("The following stacks need to be upgraded: ");
        for (Map.Entry<String, OutboundType> entry : stackOutboundTypeMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || !entry.getValue().isUpgradeable()) {
                continue;
            }
            messageBuilder.append(entry.getKey())
                    .append(" - ")
                    .append(entry.getValue())
                    .append("; ");
        }
        return messageBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StackOutboundTypeValidationV4Response that = (StackOutboundTypeValidationV4Response) o;
        return Objects.equals(getStackOutboundTypeMap(), that.getStackOutboundTypeMap()) && Objects.equals(getMessage(), that.getMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStackOutboundTypeMap(), getMessage());
    }

    @Override
    public String toString() {
        return "DefaultOutboundResponse{" +
                "stackOutboundTypeMap=" + stackOutboundTypeMap +
                '}';
    }
}
