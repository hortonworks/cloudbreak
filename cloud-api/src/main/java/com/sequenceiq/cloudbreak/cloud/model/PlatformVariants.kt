package com.sequenceiq.cloudbreak.cloud.model

class PlatformVariants(val platformToVariants: Map<Platform, Collection<Variant>>, val defaultVariants: Map<Platform, Variant>)
