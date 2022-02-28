package com.sequenceiq.cloudbreak.usage.http;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.protobuf.GeneratedMessageV3;
import com.sequenceiq.cloudbreak.streaming.model.RecordRequest;

public class UsageHttpRecordRequest extends RecordRequest {

    public UsageHttpRecordRequest(String rawBody, GeneratedMessageV3 messageBody, long timestamp, boolean forceRawOutput) {
        super(rawBody, messageBody, timestamp, forceRawOutput);
    }

    public UsageProto.Event messageBodyAsUsageEvent() {
        return getMessageBody().isPresent() ? (UsageProto.Event) getMessageBody().get() : null;
    }

    @Override
    public String toString() {
        return "UsageHttpRecordRequest{} " + super.toString();
    }
}
