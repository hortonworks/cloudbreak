package com.sequenceiq.cloudbreak.cloud.yarn;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationNameUtil {

    @Value("${cb.max.yarn.resource.name.length:}")
    private int maxResourceNameLength;

    public String decorateName(String name, String userName) {
        if (name.endsWith("-cb")) {
            name += "-" + userName;
        }
        if (!name.endsWith("-cb-" + userName)) {
            name = name + "-cb-" + userName;
        }
        return cutToMaxLength(name, userName);
    }

    private String cutToMaxLength(String name, String userName) {
        if (name.length() <= maxResourceNameLength) {
            return name;
        }
        String postfix = "-cb-" + userName;
        String clusterName = name.substring(0, name.indexOf(postfix));
        int newLength = maxResourceNameLength - postfix.length();
        clusterName = clusterName.substring(0, newLength);
        return clusterName + postfix;
    }
}
