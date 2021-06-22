package com.sequenceiq.cloudbreak.audit.model;

import static com.google.common.base.Preconditions.checkArgument;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.util.UuidUtil;

public class AttemptAuditEventResult {

    private final String id;

    private final String requestId;

    private final String actorCrn;

    private final String resultCode;

    private final String resultMessage;

    private final ResultEventData resultEventData;

    public AttemptAuditEventResult(Builder builder) {
        this.id = builder.id;
        this.requestId = builder.requestId;
        this.actorCrn = builder.actorCrn;
        this.resultCode = builder.resultCode;
        this.resultMessage = builder.resultMessage;
        this.resultEventData = builder.resultEventData;
    }

    public String getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getActorCrn() {
        return actorCrn;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public ResultEventData getResultEventData() {
        return resultEventData;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AttemptAuditEventResult{" +
                "id='" + id + '\'' +
                ", requestId='" + requestId + '\'' +
                ", actorCrn='" + actorCrn + '\'' +
                ", resultCode='" + resultCode + '\'' +
                ", resultMessage='" + resultMessage + '\'' +
                ", resultEventData=" + resultEventData +
                '}';
    }

    public static class Builder {

        private String id;

        private String requestId;

        private String actorCrn;

        private String resultCode;

        private String resultMessage;

        private ResultEventData resultEventData;

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder withActorCrn(String actorCrn) {
            this.actorCrn = actorCrn;
            return this;
        }

        public Builder withResultCode(String resultCode) {
            this.resultCode = resultCode;
            return this;
        }

        public Builder withResultMessage(String resultMessage) {
            this.resultMessage = resultMessage;
            return this;
        }

        public Builder withResultEventData(ResultEventData resultEventData) {
            this.resultEventData = resultEventData;
            return this;
        }

        public AttemptAuditEventResult build() {
            checkArgument(UuidUtil.isValid(id), "ID must be a valid UUID.");
            checkArgument(StringUtils.isNotBlank(resultCode), "Result code must be provided.");
            checkArgument(Crn.isCrn(actorCrn), "Actor user must be a valid CRN.");
            return new AttemptAuditEventResult(this);
        }
    }
}
