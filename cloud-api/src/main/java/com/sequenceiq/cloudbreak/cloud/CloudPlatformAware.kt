package com.sequenceiq.cloudbreak.cloud

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

/**
 * Defines the constants to identify a Cloud provider
 */
interface CloudPlatformAware {

    /**
     * Name of the Cloud provider

     * @return platform
     */
    fun platform(): Platform

    /**
     * Platform variant, some Cloud provider like OpenStack supports multiple Variants

     * @return variant
     */
    fun variant(): Variant

}
