package com.sequenceiq.cloudbreak.cloud.event.credential

class CredentialVerificationException(message: String, errorDetails: Exception) : RuntimeException(message, errorDetails)
