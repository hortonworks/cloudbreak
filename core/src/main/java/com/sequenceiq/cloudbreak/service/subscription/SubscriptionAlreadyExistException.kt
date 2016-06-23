package com.sequenceiq.cloudbreak.service.subscription

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException

class SubscriptionAlreadyExistException(message: String) : CloudbreakServiceException(message)
