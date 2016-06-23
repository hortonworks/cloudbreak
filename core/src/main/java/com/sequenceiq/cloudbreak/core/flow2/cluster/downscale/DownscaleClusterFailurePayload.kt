package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterScaleFailedPayload

class DownscaleClusterFailurePayload(stackId: Long?, hostGroupName: String, errorDetails: Exception) : ClusterScaleFailedPayload(stackId, hostGroupName, errorDetails)
