package com.sequenceiq.cloudbreak.cloud.aws.context

import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView

abstract class AwsStatusCheckerContext(val awsCredentialView: AwsCredentialView)
