package com.sequenceiq.cloudbreak.service.rdsconfig;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.redbeams.api.endpoint.v1.RedBeamsFlowEndpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.ClusterDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.RotateDatabaseServerSecretV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ClusterDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateEntryResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.support.SupportV4Endpoint;

@Service
public class RedbeamsClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsClientService.class);

    @Inject
    private DatabaseServerV4Endpoint redbeamsServerEndpoint;

    @Inject
    private RedBeamsFlowEndpoint redBeamsFlowEndpoint;

    @Inject
    private SupportV4Endpoint supportV4Endpoint;

    public DatabaseServerV4Response getByCrn(String dbCrn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redbeamsServerEndpoint.getByCrn(dbCrn));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to GET DatabaseServer properties by dbCrn: %s", dbCrn);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public DatabaseServerStatusV4Response create(AllocateDatabaseServerV4Request request) {
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redbeamsServerEndpoint.createInternal(request, initiatorUserCrn));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to create DatabaseServer %s", request.getName());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public DatabaseServerV4Response deleteByCrn(String crn, boolean force) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redbeamsServerEndpoint.deleteByCrn(crn, force));
        } catch (NotFoundException e) {
            String message = String.format("DatabaseServer with CRN %s was not found by Redbeams service in the deleteByCrn call.", crn);
            LOGGER.warn(message, e);
            throw e;
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to delete DatabaseServer with CRN %s", crn);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public void startByCrn(String crn) {
        try {
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redbeamsServerEndpoint.start(crn));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to start DatabaseServer with CRN %s", crn);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public void stopByCrn(String crn) {
        try {
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redbeamsServerEndpoint.stop(crn));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to stop DatabaseServer with CRN %s", crn);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public void turnOnSslOnProvider(String crn) {
        // TODO this will be implemented in the next phase
    }

    public FlowIdentifier rotateSslCert(String crn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redbeamsServerEndpoint.rotateSslCert(crn));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to rotate certificate DatabaseServer with CRN %s", crn);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public DatabaseServerStatusV4Response migrateRdsToTls(String crn) {
        try {
            // TODO this will be implemented in the next phase
            return null;
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to migrate DatabaseServer with CRN %s", crn);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public SslCertificateEntryResponse getLatestCertificate(String cloudPlatform, String region) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> supportV4Endpoint.getLatestCertificate(cloudPlatform, region));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to get certificate for %s cloudPlatform and %s region.", cloudPlatform, region);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public FlowIdentifier updateToLatestSslCert(String crn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redbeamsServerEndpoint.updateToLatestSslCert(crn));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to rotate certificate DatabaseServer with CRN %s", crn);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public UpgradeDatabaseServerV4Response upgradeByCrn(String crn, UpgradeDatabaseServerV4Request request) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redbeamsServerEndpoint.upgrade(crn, request));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to upgrade DatabaseServer with CRN %s to version %s due to error: %s",
                    crn,
                    request.getUpgradeTargetMajorVersion(),
                    e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public UpgradeDatabaseServerV4Response validateUpgrade(String crn, UpgradeDatabaseServerV4Request request) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redbeamsServerEndpoint.validateUpgrade(crn, request));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to validate upgrade DatabaseServer with CRN %s to version %s due to error: %s",
                    crn,
                    request.getUpgradeTargetMajorVersion(),
                    e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public UpgradeDatabaseServerV4Response validateUpgradeCleanup(String crn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redbeamsServerEndpoint.validateUpgradeCleanup(crn));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to clean up validate upgrade DatabaseServer with CRN %s due to error: %s",
                    crn,
                    e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public DatabaseServerV4Response getByClusterCrn(String environmentCrn, String clusterCrn) {
        validateForGetByClusterCrn(environmentCrn, clusterCrn);
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redbeamsServerEndpoint.getByClusterCrn(environmentCrn, clusterCrn));
        } catch (NotFoundException e) {
            String message = String.format("DatabaseServer with Environment CRN %s and Cluster CRN %s was not found", environmentCrn, clusterCrn);
            LOGGER.debug(message, e);
            throw e;
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to get DatabaseServer with Environment CRN %s and Cluster CRN %s", environmentCrn, clusterCrn);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public FlowIdentifier rotateSecret(RotateDatabaseServerSecretV4Request request) {
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redbeamsServerEndpoint.rotateSecret(request, initiatorUserCrn));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to rotate DatabaseServer secret %s with CRN %s due to error: %s",
                    request.getSecret(), request.getCrn(), e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private void validateForGetByClusterCrn(String environmentCrn, String clusterCrn) {
        if (StringUtils.isBlank(environmentCrn)) {
            throw new CloudbreakServiceException("Environment CRN is empty");
        }
        if (StringUtils.isBlank(clusterCrn)) {
            throw new CloudbreakServiceException("Cluster CRN is empty");
        }
    }

    public FlowCheckResponse hasFlowRunningByFlowId(String flowId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> redBeamsFlowEndpoint.hasFlowRunningByFlowId(flowId));
    }

    public FlowCheckResponse hasFlowChainRunningByFlowChainId(String flowChainId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> redBeamsFlowEndpoint.hasFlowRunningByChainId(flowChainId));
    }

    public FlowLogResponse getLastFlowId(String resourceCrn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> redBeamsFlowEndpoint.getLastFlowByResourceCrn(resourceCrn));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == HttpStatus.NOT_FOUND.value()) {
                return null;
            }
            throw e;
        }
    }

    public ClusterDatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatusByStackCrns(
            ClusterDatabaseServerCertificateStatusV4Request request, String userCrn) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> redbeamsServerEndpoint.listDatabaseServersCertificateStatusByStackCrns(request, userCrn));
    }
}