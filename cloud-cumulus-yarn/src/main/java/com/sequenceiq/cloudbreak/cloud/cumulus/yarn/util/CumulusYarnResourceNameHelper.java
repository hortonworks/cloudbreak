package com.sequenceiq.cloudbreak.cloud.cumulus.yarn.util;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Component
public class CumulusYarnResourceNameHelper {

    @Value("${cb.max.yarn.resource.name.length:}")
    private int maxResourceNameLength;

    public String getComponentNameFromGroupName(String groupName) {
        return Optional.ofNullable(groupName).map(s -> s.replaceAll("_", "-")).orElse(null);
    }

    public String createApplicationName(AuthenticatedContext ac) {
        return String.format("%s-%s", Splitter.fixedLength(maxResourceNameLength - (ac.getCloudContext().getId().toString().length() + 1))
                .splitToList(ac.getCloudContext().getName()).get(0), ac.getCloudContext().getId());
    }
}
