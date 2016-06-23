package com.sequenceiq.cloudbreak.reactor.api.event.resource

class ExtendHostMetadataRequest(stackId: Long?, upscaleCandidateAddresses: Set<String>) : AbstractClusterBootstrapRequest(stackId, upscaleCandidateAddresses)
