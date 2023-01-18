package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class RotateSaltPasswordRequest implements JsonEntity {

    @Schema(description = ModelDescriptions.ClusterModelDescription.ROTATE_SALT_PASSWORD_REASON)
    private RotateSaltPasswordReason reason;

    public RotateSaltPasswordRequest() {
    }

    public RotateSaltPasswordRequest(RotateSaltPasswordReason reason) {
        this.reason = reason;
    }

    public RotateSaltPasswordReason getReason() {
        return reason;
    }

    public void setReason(RotateSaltPasswordReason reason) {
        this.reason = reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RotateSaltPasswordRequest)) {
            return false;
        }
        RotateSaltPasswordRequest that = (RotateSaltPasswordRequest) o;
        return reason == that.reason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reason);
    }
}
