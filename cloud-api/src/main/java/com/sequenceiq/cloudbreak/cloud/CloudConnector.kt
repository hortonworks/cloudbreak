package com.sequenceiq.cloudbreak.cloud

/**
 * In order to integrate a Cloud provider into the Cloudbreak this interface needs to be implemented.  Loading of the Cloud provider specific code
 * is automatically done by Cloudbreak, if the class which implements this interface is on the classpath. Cloud providers implementations are
 * stored in a map in Cloudbreak and the [CloudPlatformAware] is used as key to identify the different implementations.
 */
interface CloudConnector : CloudPlatformAware {

    /**
     * Access to the [Authenticator] object.

     * @return the [Authenticator] object
     */
    fun authentication(): Authenticator

    /**
     * Access to the [Setup] object.

     * @return the [Setup] object
     */
    fun setup(): Setup

    /**
     * Access to the [CredentialConnector] object.

     * @return the [CredentialConnector] object
     */
    fun credentials(): CredentialConnector

    /**
     * Access to the [ResourceConnector] object.

     * @return the [ResourceConnector] object
     */
    fun resources(): ResourceConnector

    /**
     * Access to the [MetadataCollector] object.

     * @return the [MetadataCollector] object
     */
    fun instances(): InstanceConnector

    /**
     * Access to the [MetadataCollector] object.

     * @return the [MetadataCollector] object
     */
    fun metadata(): MetadataCollector

    /**
     * Access to the [PlatformParameters] object.

     * @return the [PlatformParameters] object
     */
    fun parameters(): PlatformParameters

}
