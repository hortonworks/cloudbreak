package com.sequenceiq.cloudbreak.idbmms;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.idbmms.config.IdbmmsClientConfig;
import com.sequenceiq.cloudbreak.idbmms.exception.IdbmmsOperationException;
import com.sequenceiq.cloudbreak.idbmms.model.MappingsConfig;

/**
 * A GRPC-based client for the IDBroker Mapping Management Service (IDBMMS).
 */
@Component
public class GrpcIdbmmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcIdbmmsClient.class);

    @Qualifier("idbmmsManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private IdbmmsClientConfig idbmmsClientConfig;

    public static GrpcIdbmmsClient createClient(ManagedChannelWrapper channelWrapper, IdbmmsClientConfig idbmmsClientConfig) {
        GrpcIdbmmsClient client = new GrpcIdbmmsClient();
        client.channelWrapper = checkNotNull(channelWrapper, "channelWrapper should not be null.");
        client.idbmmsClientConfig = checkNotNull(idbmmsClientConfig, "clientConfig should not be null.");
        return client;
    }

    /**
     * Retrieves IDBroker mappings from IDBMMS for a particular environment.
     *
     * @param actorCrn       the actor CRN; must not be {@code null}
     * @param environmentCrn the environment CRN to get mappings for; must not be {@code null}
     * @return the mappings config associated with environment {@code environmentCrn}; never {@code null}
     * @throws NullPointerException     if either argument is {@code null}
     * @throws IdbmmsOperationException if any problem is encountered during the IDBMMS call processing
     */
    public MappingsConfig getMappingsConfig(String actorCrn, String environmentCrn) {
        checkNotNull(actorCrn, "actor Crn should not be null.");
        checkNotNull(environmentCrn, "environment Crn should not be null.");
        try {
            IdbmmsClient client = makeClient(actorCrn);
            LOGGER.debug("Fetching IDBroker mappings for environment {} with user {}.", environmentCrn, actorCrn);
            MappingsConfig mappingsConfig = client.getMappingsConfig(environmentCrn);
            LOGGER.debug("Retrieved IDBroker mappings with version {} for environment {}.", mappingsConfig.getMappingsVersion(), environmentCrn);
            return mappingsConfig;
        } catch (RuntimeException e) {
            throw new IdbmmsOperationException(String.format("Error during getting IDBroker mappings configuration: %s", e.getMessage()), e);
        }
    }

    /**
     * Deletes IDBroker mappings in IDBMMS for a particular environment.
     *
     * @param actorCrn       the actor CRN; must not be {@code null}
     * @param environmentCrn the environment CRN to delete mappings for; must not be {@code null}
     * @return delete mappings response
     * @throws NullPointerException     if either argument is {@code null}
     * @throws IdbmmsOperationException if any problem is encountered during the IDBMMS call processing
     */
    public DeleteMappingsResponse deleteMappings(String actorCrn, String environmentCrn) {
        checkNotNull(actorCrn, "actor Crn should not be null.");
        checkNotNull(environmentCrn, "environment Crn should not be null.");
        try {
            IdbmmsClient client = makeClient(actorCrn);
            LOGGER.debug("Deleting IDBroker mappings from environment {} with user {}.", environmentCrn, actorCrn);
            DeleteMappingsResponse deleteResponse = client.deleteMappings(environmentCrn);
            LOGGER.debug("Deleted IDBroker mappings from environment {} with {}.", environmentCrn, deleteResponse);
            return deleteResponse;
        } catch (RuntimeException e) {
            throw new IdbmmsOperationException(String.format("Error during deleting IDBroker mappings: %s", e.getMessage()), e);
        }
    }

    /**
     * Sets IDBroker mappings in IDBMMS for a particular environment.
     *
     * @param actorCrn                   the actor CRN; must not be {@code null}
     * @param environmentCrn             the environment CRN to delete mappings for; must not be {@code null}
     * @param dataAccessRole             the cloud provider role to which data access services will be mapped; must not be {@code null}
     * @param baselineRole               the cloud provider role associated with the baseline instance identity,
     *                                   that write to cloud storage will be mapped to this role; must not be {@code null}
     * @param rangerAccessAuthorizerRole the cloud provider role to which the Ranger RAZ service will be mapped
     * @return set mappings response associated with environment {@code environmentCrn}; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     * @throws IdbmmsOperationException if any problem is encountered during the IDBMMS call processing
     */
    public SetMappingsResponse setMappings(String actorCrn, String environmentCrn, String dataAccessRole,
            String baselineRole, String rangerAccessAuthorizerRole) {
        checkNotNull(actorCrn, "actor Crn should not be null.");
        checkNotNull(environmentCrn, "environment Crn should not be null.");
        checkNotNull(dataAccessRole, "data access role should not be null.");
        checkNotNull(baselineRole, "baseline role should not be null.");

        try {
            IdbmmsClient client = makeClient(actorCrn);
            LOGGER.debug("Setting IDBroker mappings for environment {} with user {}.", environmentCrn, actorCrn);
            SetMappingsResponse mappings = client.setMappings(environmentCrn, dataAccessRole, baselineRole, rangerAccessAuthorizerRole);
            LOGGER.debug("IDBroker mappings {} for environment {} has been set.", mappings, environmentCrn);
            return mappings;
        } catch (RuntimeException e) {
            throw new IdbmmsOperationException(String.format("Error during setting new IDBroker mappings for environment: %s", e.getMessage()), e);
        }
    }

    /**
     * Retrieves IDBroker mappings from IDBMMS for a particular environment.
     *
     * @param actorCrn       the actor CRN; must not be {@code null}
     * @param environmentCrn the environment CRN to get mappings for; must not be {@code null}
     * @return get mappings response associated with environment {@code environmentCrn}; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     * @throws IdbmmsOperationException if any problem is encountered during the IDBMMS call processing
     */
    public GetMappingsResponse getMappings(String actorCrn, String environmentCrn) {
        checkNotNull(actorCrn, "actor Crn should not be null.");
        checkNotNull(environmentCrn, "actorCrn should not be null.");

        try {
            IdbmmsClient client = makeClient(actorCrn);
            LOGGER.debug("Fetching IDBroker mappings for environment {} with user {}.", environmentCrn, actorCrn);
            GetMappingsResponse mappings = client.getMappings(environmentCrn);
            LOGGER.debug("Retrieved IDBroker mappings {} for environment {}.", mappings, environmentCrn);
            return mappings;
        } catch (RuntimeException e) {
            throw new IdbmmsOperationException(String.format("Error during getting IDBroker mappings: %s", e.getMessage()), e);
        }
    }

    IdbmmsClient makeClient(String actorCrn) {
        return new IdbmmsClient(channelWrapper.getChannel(), idbmmsClientConfig, actorCrn);
    }
}
