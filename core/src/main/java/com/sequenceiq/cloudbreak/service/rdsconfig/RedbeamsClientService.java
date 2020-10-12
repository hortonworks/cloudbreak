package com.sequenceiq.cloudbreak.service.rdsconfig;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
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

    public DatabaseServerV4Response getByCrn(String dbCrn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(() -> redbeamsServerEndpoint.getByCrn(dbCrn));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to GET DatabaseServer properties by dbCrn: %s", dbCrn);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public DatabaseServerStatusV4Response create(AllocateDatabaseServerV4Request request) {
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            return ThreadBasedUserCrnProvider.doAsInternalActor(() -> redbeamsServerEndpoint.createInternal(request, initiatorUserCrn));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to create DatabaseServer %s", request.getName());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public DatabaseServerV4Response deleteByCrn(String crn, boolean force) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(() -> redbeamsServerEndpoint.deleteByCrn(crn, force));
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to delete DatabaseServer with CRN %s", crn);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
