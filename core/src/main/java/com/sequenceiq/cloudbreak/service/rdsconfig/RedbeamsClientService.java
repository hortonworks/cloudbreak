package com.sequenceiq.cloudbreak.service.rdsconfig;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Service
public class RedbeamsClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsClientService.class);

    @Inject
    private DatabaseServerV4Endpoint redbeamsServerEndpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public DatabaseServerV4Response getByCrn(String dbCrn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
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
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
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
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
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
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
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
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> redbeamsServerEndpoint.stop(crn));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to stop DatabaseServer with CRN %s", crn);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public DatabaseServerV4Response getByClusterCrn(String environmentCrn, String clusterCrn) {
        validateForGetByClusterCrn(environmentCrn, clusterCrn);
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
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

    private void validateForGetByClusterCrn(String environmentCrn, String clusterCrn) {
        if (StringUtils.isBlank(environmentCrn)) {
            throw new CloudbreakServiceException("Environment CRN is empty");
        }
        if (StringUtils.isBlank(clusterCrn)) {
            throw new CloudbreakServiceException("Cluster CRN is empty");
        }
    }
}
