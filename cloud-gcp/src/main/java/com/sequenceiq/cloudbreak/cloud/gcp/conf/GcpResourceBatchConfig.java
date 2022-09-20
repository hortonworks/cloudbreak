package com.sequenceiq.cloudbreak.cloud.gcp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.ResourceBatchConfig;

@Service
public class GcpResourceBatchConfig implements ResourceBatchConfig {

    @Value("${cb.gcp.stopStart.batch.size}")
    private Integer stopStartBatchSize;

    @Value("${cb.gcp.create.batch.size}")
    private Integer createBatchSize;

    public Integer stopStartBatchSize() {
        return stopStartBatchSize;
    }

    public Integer createBatchSize() {
        return createBatchSize;
    }

    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }
}