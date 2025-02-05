package com.sequenceiq.cloudbreak.cloud.gcp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.ResourceCreateBatchConfig;

@Service
public class GcpResourceCreateBatchConfig implements ResourceCreateBatchConfig {

    @Value("${cb.gcp.create.batch.size}")
    private Integer createBatchSize;

    public Integer batchSize() {
        return createBatchSize;
    }

    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }
}