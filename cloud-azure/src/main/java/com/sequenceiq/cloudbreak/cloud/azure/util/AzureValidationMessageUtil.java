package com.sequenceiq.cloudbreak.cloud.azure.util;

import com.sequenceiq.common.model.CloudIdentityType;

public class AzureValidationMessageUtil {

    private static final String ASSUMER_IDENTITY = "Assumer";

    private static final String LOG_IDENTITY = "Log";

    private static final String ADVICE_MESSAGE = "Please check if you've used the correct %s when setting up %s.";

    private static final String DATA_ACCESS = "Data Access";

    private static final String LOGS = "Logs-Storage and Audit";

    private static final String DEFAULT_PLACE = "the system";

    private AzureValidationMessageUtil() {
    }

    public static String getIdentityType(CloudIdentityType cloudIdentityType) {
        String result = "";
        if (CloudIdentityType.ID_BROKER.equals(cloudIdentityType)) {
            result = ASSUMER_IDENTITY;
        } else if (CloudIdentityType.LOG.equals(cloudIdentityType)) {
            result = LOG_IDENTITY;
        }
        return result;
    }

    public static String getAdviceMessage(AzureMessageResourceType type, CloudIdentityType cloudidentityType) {
        return String.format(ADVICE_MESSAGE, type.getMessage(), getPlace(cloudidentityType));
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

    public enum AzureMessageResourceType {
        IDENTITY("Identity"),
        STORAGE_LOCATION("Storage Location");

        private String message;

        AzureMessageResourceType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
