package com.sequenceiq.cloudbreak.reactor.api.event.resource

class BootstrapNewNodesRequest(stackId: Long?, upscaleCandidateAddresses: Set<String>) : AbstractClusterBootstrapRequest(stackId, upscaleCandidateAddresses)
