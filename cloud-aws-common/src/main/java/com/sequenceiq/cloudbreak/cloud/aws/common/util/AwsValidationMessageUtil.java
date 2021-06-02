package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAccessConfigType;
import com.sequenceiq.common.model.CloudIdentityType;

public class AwsValidationMessageUtil {

    public static final String ADVICE_MESSAGE = "Please check if you've used the correct %s when setting up %s.";

    private static final String INSTANCE_PROFILE = "Instance profile";

    private static final String ROLE = "Role";

    private static final String DEFAULT_RESOURCE = "Resource name";

    private static final String DEFAULT_PLACE = "the system";

    private static final String DATA_ACCESS = "Data Access";

    private static final String LOGS = "Logs-Storage and Audit";

    private AwsValidationMessageUtil() {
    }

    public static String getAdviceMessage(AwsAccessConfigType resource, CloudIdentityType cloudidentityType) {
        return String.format(ADVICE_MESSAGE, getResourceText(resource), getPlace(cloudidentityType));
    }

    private static String getPlace(CloudIdentityType cloudidentityType) {
        switch (cloudidentityType) {
            case ID_BROKER:
                return DATA_ACCESS;
            case LOG:
                return LOGS;
            default:
                return DEFAULT_PLACE;
        }
    }

    private static String getResourceText(AwsAccessConfigType resource) {
        switch (resource) {
            case INSTANCE_PROFILE:
                return INSTANCE_PROFILE;
            case ROLE:
                return ROLE;
            default:
                return DEFAULT_RESOURCE;
        }
    }
}