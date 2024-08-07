package com.sequenceiq.environment.environment.service.database;

import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.environment.exception.RedbeamsOperationFailedException;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerCertificateStatusV4Responses;

@Service
public class RedBeamsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedBeamsService.class);

    private final DatabaseServerV4Endpoint databaseServerV4Endpoint;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public RedBeamsService(DatabaseServerV4Endpoint databaseServerV4Endpoint,
        WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.databaseServerV4Endpoint = databaseServerV4Endpoint;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    public DatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatus(EnvironmentDatabaseServerCertificateStatusV4Request request) {
        try {
            DatabaseServerCertificateStatusV4Request databaseServerCertificateStatusV4Request = new DatabaseServerCertificateStatusV4Request();
            databaseServerCertificateStatusV4Request.setEnvironmentCrns(request.getEnvironmentCrns());
            return databaseServerV4Endpoint.listDatabaseServersCertificateStatus(databaseServerCertificateStatusV4Request);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to get DatabaseServersCertificateStatus for environments '%s' due to: '%s'",
                    request.getEnvironmentCrns(), errorMessage), e);
            throw new RedbeamsOperationFailedException(errorMessage, e);
        }
    }
}
