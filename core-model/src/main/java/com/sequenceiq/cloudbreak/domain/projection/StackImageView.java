package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.common.json.Json;

public interface StackImageView {

    Long getId();

    Json getImage();
}
