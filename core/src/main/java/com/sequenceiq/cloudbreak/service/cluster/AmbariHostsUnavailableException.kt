package com.sequenceiq.cloudbreak.service.cluster

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException

class AmbariHostsUnavailableException(message: String) : CloudbreakServiceException(message)
