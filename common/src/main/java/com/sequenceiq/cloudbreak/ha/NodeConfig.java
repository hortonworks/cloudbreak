package com.sequenceiq.cloudbreak.ha;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NodeConfig {

    @Value("${instance.node.id:}")
    private String id;

    @Value("${instance.uuid:}")
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