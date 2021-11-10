package com.sequenceiq.cloudbreak.kudu.model;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.protobuf.GeneratedMessageV3;
import com.sequenceiq.cloudbreak.streaming.model.RecordRequest;

public class KuduRecordRequest extends RecordRequest {

    private static final String DEFAULT_KUDU_TABLE_TYPE = "impala";

    private final String database;

    private final String table;

    private final String type;

    private final Map<String, Object> additionalColumns;

    public KuduRecordRequest(Builder builder) {
        super(builder.rawBody, builder.messageBody);
        this.database = builder.database;
        this.table = builder.table;
        this.additionalColumns = builder.additionalColumns;
        this.type = StringUtils.isBlank(builder.type) ? DEFAULT_KUDU_TABLE_TYPE : builder.type;
    }

    public String getDatabase() {
        return database;
    }

    public String getTable() {
        return table;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getAdditionalColumns() {
        return additionalColumns;
    }

    @Override
    public String toString() {
        return "KuduRecordRequest{" +
                "database='" + database + '\'' +
                ", table='" + table + '\'' +
                ", type='" + type + '\'' +
                "} " + super.toString();
    }

    public static class Builder {

        private String database;

        private String table;

        private String type;

        private Map<String, Object> additionalColumns;

        private String rawBody;

        private GeneratedMessageV3 messageBody;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public KuduRecordRequest build() {
            return new KuduRecordRequest(this);
        }

        public Builder withRawBody(String rawBody) {
            this.rawBody = rawBody;
            return this;
        }

        public Builder withMessageBody(GeneratedMessageV3 messageBody) {
            this.messageBody = messageBody;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withTable(String table) {
            this.table = table;
            return this;
        }

        public Builder withDatabase(String database) {
            this.database = database;
            return this;
        }

        public  Builder withAdditionalColumns(Map<String, Object> additionalColumns) {
            this.additionalColumns = additionalColumns;
            return this;
        }

    }
}
