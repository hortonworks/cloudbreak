package com.sequenceiq.cloudbreak.cloud.aws.view;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceProfileStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class AwsInstanceProfileView {

    private final Map<String, String> parameters;

    public AwsInstanceProfileView(CloudStack stack) {
        parameters = stack.getParameters();
    }

    public boolean isEnableInstanceProfileStrategy() {
        return parameters.containsKey(AwsPlatformParameters.INSTANCE_PROFILE_STRATEGY)
                && !isEmpty(String.valueOf(parameters.get(AwsPlatformParameters.INSTANCE_PROFILE_STRATEGY)));
    }

    public boolean isInstanceProfileAvailable() {
        return parameters.containsKey(AwsPlatformParameters.INSTANCE_PROFILE)
                && !isEmpty(String.valueOf(parameters.get(AwsPlatformParameters.INSTANCE_PROFILE)));
    }

    public String getInstanceProfile() {
        return String.valueOf(parameters.get(AwsPlatformParameters.INSTANCE_PROFILE));
    }

    public InstanceProfileStrategy getInstanceProfileStrategy() {
        String instanceProfileStrategy = parameters.get(AwsPlatformParameters.INSTANCE_PROFILE_STRATEGY);
        return instanceProfileStrategy == null ? null : InstanceProfileStrategy.valueOf(instanceProfileStrategy);
    }

    public boolean isCreateInstanceProfile() {
        return InstanceProfileStrategy.CREATE.equals(getInstanceProfileStrategy());
    }

    public boolean isUseExistingInstanceProfile() {
        return InstanceProfileStrategy.USE_EXISTING.equals(getInstanceProfileStrategy());
    }

}
