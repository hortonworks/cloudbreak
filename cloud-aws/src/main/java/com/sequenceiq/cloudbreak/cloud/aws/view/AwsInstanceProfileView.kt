package com.sequenceiq.cloudbreak.cloud.aws.view

import org.apache.commons.lang3.StringUtils.isEmpty

import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformParameters
import com.sequenceiq.cloudbreak.api.model.InstanceProfileStrategy

class AwsInstanceProfileView(private val parameters: Map<String, String>) {

    val isEnableInstanceProfileStrategy: Boolean
        get() = parameters.containsKey(AwsPlatformParameters.INSTANCE_PROFILE_STRATEGY) && !isEmpty(parameters[AwsPlatformParameters.INSTANCE_PROFILE_STRATEGY].toString())

    val isS3RoleAvailable: Boolean
        get() = parameters.containsKey(AwsPlatformParameters.S3_ROLE) && !isEmpty(parameters[AwsPlatformParameters.S3_ROLE].toString())

    val s3Role: String
        get() = parameters[AwsPlatformParameters.S3_ROLE].toString()

    val instanceProfileStrategy: InstanceProfileStrategy?
        get() {
            val instanceProfileStrategy = parameters[AwsPlatformParameters.INSTANCE_PROFILE_STRATEGY]
            return if (instanceProfileStrategy == null) null else InstanceProfileStrategy.valueOf(instanceProfileStrategy)
        }

    val isCreateInstanceProfile: Boolean
        get() = InstanceProfileStrategy.CREATE == instanceProfileStrategy

    val isUseExistingInstanceProfile: Boolean
        get() = InstanceProfileStrategy.USE_EXISTING == instanceProfileStrategy

}
