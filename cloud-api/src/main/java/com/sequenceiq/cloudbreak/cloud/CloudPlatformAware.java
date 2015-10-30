package com.sequenceiq.cloudbreak.cloud;

/**
 * Defines the constants to identify a Cloud provider
 */
public interface CloudPlatformAware {

    /**
     * Name of the Cloud provider
     *
     * @return platform
     */
    String platform();

    /**
     * Platform variant, some Cloud provider like OpenStack supports multiple Variants
     *
     * @return variant
     */
    String variant();
}
