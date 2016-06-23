package com.sequenceiq.cloudbreak.cloud.aws.view

import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformParameters

class AwsGroupView(val instanceCount: Int?, val type: String, val flavor: String, val groupName: String, val volumeCount: Int?,
                   val ebsEncrypted: Boolean?, val volumeSize: Int?, val volumeType: String, val spotPrice: Double?) {

    val ebsOptimized: Boolean?
        get() = AwsPlatformParameters.AwsDiskType.St1.value() == volumeType
}
