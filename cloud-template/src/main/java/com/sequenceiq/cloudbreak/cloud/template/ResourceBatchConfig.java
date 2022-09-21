package com.sequenceiq.cloudbreak.cloud.template;

import com.sequenceiq.cloudbreak.cloud.model.Variant;

public interface ResourceBatchConfig {

    Integer stopStartBatchSize();

    Integer createBatchSize();

    Variant variant();
}