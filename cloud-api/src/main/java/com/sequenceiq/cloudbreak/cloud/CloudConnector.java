package com.sequenceiq.cloudbreak.cloud;

import java.util.List;
import java.util.Optional;

import com.sequenceiq.cloudbreak.common.mappable.StorageType;

/**
 * In order to integrate a Cloud provider into the Cloudbreak this interface needs to be implemented.  Loading of the Cloud provider specific code
 * is automatically done by Cloudbreak, if the class which implements this interface is on the classpath. Cloud providers implementations are
 * stored in a map in Cloudbreak and the {@link CloudPlatformAware} is used as key to identify the different implementations.
 */
public interface CloudConnector extends CloudPlatformAware {

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
     * @return the available {@link Validator} objects
     * @param validatorType Describes the type of validator to use
     */
    List<Validator> validators(ValidatorType validatorType);

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
    ResourceConnector resources();

    /**
     * Access to the {@link InstanceConnector} object.
     *
     * @return the {@link InstanceConnector} object
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

    /**
     *  Access to the {@link EncryptionResources} object.
     *
     *  @return the {@link EncryptionResources} object
     */
    EncryptionResources encryptionResources();

    /**
     * Giving back a valid provider display name. (because of CB-5013)
     *
     * @return the string object
     */
    default String regionToDisplayName(String region) {
        return region;
    }

    /**
     * Giving back a valid provider region. (because of CB-5013)
     *
     * @return the string object
     */
    default String displayNameToRegion(String displayName) {
        return regionToDisplayName(displayName);
    }

    /**
     * Access to the {@link IdentityService} object.
     *
     * @return the {@link IdentityService} object
     */
    default IdentityService identityService() {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    /**
     * Access to the {@link ObjectStorageConnector} object.
     *
     * @return the {@link ObjectStorageConnector} object
     */
    default ObjectStorageConnector objectStorage() {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    /**
     * Access to the {@link ConsumptionCalculator} object.
     *
     * @return the {@link ConsumptionCalculator} object
     */
    default Optional<ConsumptionCalculator> consumptionCalculator(StorageType storageType) {
        return Optional.empty();
    }

    /**
     * Access to the {@link NoSqlConnector} object.
     *
     * @return the {@link NoSqlConnector} object
     */

    default NoSqlConnector noSql() {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    /**
     * Access to the {@link PublicKeyConnector} object.
     *
     * @return the {@link PublicKeyConnector} object
     */
    default PublicKeyConnector publicKey() {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    /**
     * Access to the {@link AvailabilityZoneConnector} object.
     *
     * @return the {@link AvailabilityZoneConnector} object
     */

    default AvailabilityZoneConnector availabilityZoneConnector() {
        return null;
    }

    /**
     * Access to the {@link ResourceVolumeConnector} object.
     *
     * @return the {@link ResourceVolumeConnector} object
     */
    ResourceVolumeConnector volumeConnector();

    /**
     * Access to the {@link SecretConnector} object.
     *
     * @return the {@link SecretConnector} object
     */
    default SecretConnector secretConnector() {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default ScriptResources scriptResources() {
        return () -> null;
    }

    default Optional<NotificationConnector> notifications() {
        return Optional.empty();
    }

}
