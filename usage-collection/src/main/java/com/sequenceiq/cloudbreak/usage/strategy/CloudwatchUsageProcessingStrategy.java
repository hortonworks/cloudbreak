package com.sequenceiq.cloudbreak.usage.strategy;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.cloudwatch.model.CloudwatchRecordRequest;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.usage.processor.EdhCloudwatchConfiguration;
import com.sequenceiq.cloudbreak.usage.processor.EdhCloudwatchField;
import com.sequenceiq.cloudbreak.usage.processor.EdhCloudwatchProcessor;

/**
 * Cloudwatch usage reporter strategy class that transfer events to cloudwatch log streams for specific log group.
 */
@Service
public class CloudwatchUsageProcessingStrategy implements UsageProcessingStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudwatchUsageProcessingStrategy.class);

    private final EdhCloudwatchProcessor edhCloudwatchProcessor;

    private final EdhCloudwatchConfiguration edhCloudwatchConfiguration;

    private final Map<String, Object> additionalFields;

    public CloudwatchUsageProcessingStrategy(EdhCloudwatchProcessor edhCloudwatchProcessor, EdhCloudwatchConfiguration edhCloudwatchConfiguration) {
        this.edhCloudwatchProcessor = edhCloudwatchProcessor;
        this.edhCloudwatchConfiguration = edhCloudwatchConfiguration;
        this.additionalFields = toMap(edhCloudwatchConfiguration.getAdditionalFields());
    }

    @Override
    public void processUsage(UsageProto.Event event) {
        LOGGER.info("Logging binary format for the following usage event: {}", event);
        Map<String, Object> fields = new HashMap<>();
        long timestamp = event.getTimestamp();
        String binaryUsageEvent = BaseEncoding.base64().encode(event.toByteArray());
        fields.put("@message", String.format("CDP_BINARY_USAGE_EVENT - %s", binaryUsageEvent));
        fields.put("@timestamp", Instant.now().toEpochMilli());
        if (MapUtils.isNotEmpty(additionalFields)) {
            fields.putAll(additionalFields);
        }
        String jsonMessageInput = JsonUtil.createJsonTree(fields).toString();
        CloudwatchRecordRequest recordRequest = CloudwatchRecordRequest.Builder.newBuilder()
                .withTimestamp(timestamp)
                .withRawBody(jsonMessageInput)
                .withMessageBody(event)
                .withForceRawOutput(true)
                .build();
        edhCloudwatchProcessor.processRecord(recordRequest);
    }

    public boolean isEnabled() {
        return edhCloudwatchConfiguration.isEnabled();
    }

    private Map<String, Object> toMap(List<EdhCloudwatchField> additionalFields) {
        Map<String, Object> map = new HashMap<>();
        for (EdhCloudwatchField field : additionalFields) {
            map.put(field.getKey(), field.getValue());
        }
        return map;
    }
}
