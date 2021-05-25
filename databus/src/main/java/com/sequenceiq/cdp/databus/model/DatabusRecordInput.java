package com.sequenceiq.cdp.databus.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

public class DatabusRecordInput {

    private final PutRecordRequest recordRequest;

    private final DatabusRequestContext databusRequestContext;

    private DatabusRecordInput(Builder builder) {
        this.databusRequestContext = builder.databusRequestContext;
        this.recordRequest = createPutRecordRequest(builder);
    }

    private PutRecordRequest createPutRecordRequest(Builder builder) {
        PutRecordRequest putRecordRequest = new PutRecordRequest();
        Record record = new Record();
        if (StringUtils.isNotBlank(builder.payload)) {
            byte[] payloadBytes = builder.payload.getBytes(StandardCharsets.UTF_8);
            String encodedPayload = BaseEncoding.base64().encode(payloadBytes);
            record.setPayload(encodedPayload);
            record.setPayloadSize(payloadBytes.length);
        }
        record.setStreamName(builder.databusStreamConfiguration.getDbusStreamName());
        List<Header> headers = new ArrayList<>();
        if (MapUtils.isNotEmpty(builder.databusRequestContext.getAdditionalDatabusHeaders())) {
            for (Map.Entry<String, String> entry : builder.databusRequestContext.getAdditionalDatabusHeaders().entrySet()) {
                Header header = new Header(entry.getKey(), entry.getValue());
                headers.add(header);
            }
        }
        String appName = builder.databusStreamConfiguration.getDbusAppName();
        headers.add(new Header("app", appName));
        headers.add(new Header(builder.databusStreamConfiguration.getDbusAppNameKey(), appName));
        record.setHeaders(headers);
        record.setPartitionKey("1");
        putRecordRequest.setRecord(record);
        return putRecordRequest;
    }

    public Optional<PutRecordRequest> getRecordRequest() {
        return Optional.ofNullable(recordRequest);
    }

    public Optional<DatabusRequestContext> getDatabusRequestContext() {
        return Optional.ofNullable(databusRequestContext);
    }

    @Override
    public String toString() {
        return "DatabusRecordInput{" +
                "recordRequest=" + recordRequest +
                ", databusRequestContext=" + databusRequestContext +
                '}';
    }

    public static class Builder {

        private String payload;

        private AbstractDatabusStreamConfiguration databusStreamConfiguration;

        private DatabusRequestContext databusRequestContext;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public DatabusRecordInput build() {
            return new DatabusRecordInput(this);
        }

        public Builder withPayload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder withDatabusStreamConfiguration(AbstractDatabusStreamConfiguration databusStreamConfiguration) {
            this.databusStreamConfiguration = databusStreamConfiguration;
            return this;
        }

        public Builder withDatabusRequestContext(DatabusRequestContext databusRequestContext) {
            this.databusRequestContext = databusRequestContext;
            return this;
        }

    }
}
