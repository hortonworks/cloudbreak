package com.sequenceiq.cloudbreak.cloud.yarn;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Component
public class ApplicationNameUtil {

    @Value("${cb.max.yarn.resource.name.length:}")
    private int maxResourceNameLength;

    public String createApplicationName(AuthenticatedContext authenticatedContext) {
        List<String> applicationNameAndUser = initializeApplicationName(authenticatedContext.getCloudContext());
        return decorateName(applicationNameAndUser.get(0), applicationNameAndUser.get(1));
    }

    public String createLoadBalancerName(AuthenticatedContext authenticatedContext) {
        List<String> applicationNameAndUser = initializeApplicationName(authenticatedContext.getCloudContext());
        return decorateLoadBalancerName(applicationNameAndUser.get(0), applicationNameAndUser.get(1));
    }

    public String decorateName(String name, String userName) {
        if (name.endsWith("-cb")) {
            name = name + "-" + userName;
        }
        if (!name.endsWith("-cb-" + userName)) {
            name = name + "-cb-" + userName;
        }
        return cutToMaxLength(name, userName, "-cb-");
    }

    public String decorateLoadBalancerName(String name, String userName) {
        if (name.endsWith("-lb")) {
            name = name + "-" + userName;
        }
        if (!name.endsWith("-lb-" + userName)) {
            name = name + "-lb-" + userName;
        }
        return cutToMaxLength(name, userName, "-lb-");
    }

    public String createLoadBalancerComponentName(String applicationName, LoadBalancerType type) {
        return applicationName + type.name();
    }

    private List<String> initializeApplicationName(CloudContext cloudContext) {
        String name = cloudContext.getName();
        String id = cloudContext.getId().toString();
        name += "-" + id;
        String user = cloudContext.getUserName().split("@")[0].replaceAll("[^a-z0-9-_]", "");
        return List.of(name, user);
    }

    private String cutToMaxLength(String name, String userName, String postfixBeginning) {
        if (name.length() <= maxResourceNameLength) {
            return name;
        }
        String postfix = postfixBeginning + userName;
        String clusterName = name.substring(0, name.indexOf(postfix));
        int newLength = maxResourceNameLength - postfix.length();
        clusterName = clusterName.substring(0, newLength);
        return clusterName + postfix;
    }
}
