package com.sequenceiq.cloudbreak.cloud.gcp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.ResourceStopStartBatchConfig;

@Service
public class GcpResourceStopStartBatchConfig implements ResourceStopStartBatchConfig {

    @Value("${cb.gcp.stopStart.batch.size}")
    private Integer stopStartBatchSize;

    public Integer batchSize() {
        return stopStartBatchSize;
    }

    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }
}