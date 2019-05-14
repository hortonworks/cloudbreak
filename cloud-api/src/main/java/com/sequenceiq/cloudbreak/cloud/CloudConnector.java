package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

/**
 * In order to integrate a Cloud provider into the Cloudbreak this interface needs to be implemented.  Loading of the Cloud provider specific code
 * is automatically done by Cloudbreak, if the class which implements this interface is on the classpath. Cloud providers implementations are
 * stored in a map in Cloudbreak and the {@link CloudPlatformAware} is used as key to identify the different implementations.
 */
public interface CloudConnector<R> extends CloudPlatformAware {

    /**
     * Access to the {@link Authenticator} object.
     *
     * @return the {@link Authenticator} object
     */
    Authenticator authentication();

    /**
     * Access to the {@link Setup} object.
     *
     * @return the {@link Setup} object
     */
    Setup setup();

    /**
     * Access to the available {@link Validator}s.
     *
     * @return the available {@link Validator}s object
     */
    List<Validator> validators();

    /**
     * Access to the {@link CredentialConnector} object.
     *
     * @return the {@link CredentialConnector} object
     */
    CredentialConnector credentials();

    /**
     * Access to the {@link ResourceConnector} object.
     *
     * @return the {@link ResourceConnector} object
     */
    ResourceConnector<R> resources();

    /**
     * Access to the {@link MetadataCollector} object.
     *
     * @return the {@link MetadataCollector} object
     */
    InstanceConnector instances();

    /**
     * Access to the {@link MetadataCollector} object.
     *
     * @return the {@link MetadataCollector} object
     */
    MetadataCollector metadata();

    /**
     * Access to the {@link PlatformParameters} object.
     *
     * @return the {@link PlatformParameters} object
     */
    PlatformParameters parameters();

    /**
     * Access to the {@link PlatformResources} object.
     *
     * @return the {@link PlatformResources} object
     */
    PlatformResources platformResources();

    /**
     * Access to the {@link CloudConstant} object.
     *
     * @return the {@link CloudConstant} object
     */
    CloudConstant cloudConstant();

    /**
     * Access to the {@link NetworkConnector} object.
     *
     * @return the {@link NetworkConnector} object
     */
    NetworkConnector networkConnector();

}
