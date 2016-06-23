package com.sequenceiq.cloudbreak.cloud.aws

import org.apache.commons.lang3.StringUtils.isEmpty

import org.springframework.stereotype.Component

@Component
class AwsEnvironmentVariableChecker {

    val isAwsSecretAccessKeyAvailable: Boolean
        get() = !isEmpty(System.getenv("AWS_SECRET_ACCESS_KEY"))

    val isAwsAccessKeyAvailable: Boolean
        get() = !isEmpty(System.getenv("AWS_ACCESS_KEY_ID"))

}
