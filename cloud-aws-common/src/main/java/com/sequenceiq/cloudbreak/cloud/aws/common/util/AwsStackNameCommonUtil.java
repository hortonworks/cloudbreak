package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Component
public class AwsStackNameCommonUtil {

    private static final Logger LOGGER = getLogger(AwsStackNameCommonUtil.class);

    private static final String INSTANCE_NAME_PATTERN = "%s-%s%d";

    @Value("${cb.max.aws.resource.name.length:}")
    private int maxResourceNameLength;

    public String getInstanceName(AuthenticatedContext ac, String groupName, Long privateId) {
        if (StringUtils.isBlank(groupName)) {
            throw new IllegalArgumentException("Group name cannot be empty, instance name cannot be generated");
        }
        int lengthOfPostFix = String.valueOf(privateId).length() + groupName.length();
        int maxLengthOfName = maxResourceNameLength - lengthOfPostFix;
        String stackName = ac.getCloudContext().getName();
        if (stackName.length() > maxLengthOfName) {
            stackName = stackName.substring(0, maxLengthOfName - 1);
        }

        String instanceName = String.format(INSTANCE_NAME_PATTERN, stackName, groupName, privateId);
        LOGGER.debug("Generated instance name: {}", instanceName);
        return instanceName;
    }
}
