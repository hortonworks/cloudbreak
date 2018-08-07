package com.sequenceiq.cloudbreak.ha;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudbreakNodeConfig {

    @Value("${cb.instance.node.id:}")
    private String id;

    @Value("${cb.instance.uuid:}")
    private String instanceUUID;

    public String getId() {
        return isNodeIdSpecified() ? id : null;
    }

    public boolean isNodeIdSpecified() {
        return StringUtils.isNoneBlank(id);
    }

    public String getInstanceUUID() {
        return instanceUUID;
    }

}
