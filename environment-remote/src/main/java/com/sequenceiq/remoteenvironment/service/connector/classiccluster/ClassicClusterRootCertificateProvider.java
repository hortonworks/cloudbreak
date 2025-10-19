package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.remoteenvironment.exception.OnPremCMApiException;

@Component
class ClassicClusterRootCertificateProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassicClusterRootCertificateProvider.class);

    @Inject
    private ClassicClusterClouderaManagerApiClientProvider apiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    public GetRootCertificateResponse getRootCertificate(OnPremisesApiProto.Cluster cluster) {
        try {
            ApiClient apiClient = apiClientProvider.getClouderaManagerV51Client(cluster);
            File pem = clouderaManagerApiFactory.getCertManagerResourceApi(apiClient).getTruststore("PEM");

            GetRootCertificateResponse response = new GetRootCertificateResponse();
            response.setContents(getRootCertificateFromFile(pem));
            return response;
        } catch (ApiException e) {
            String message = "Failed to get truststore from Cloudera Manager";
            LOGGER.error(message, e);
            throw new OnPremCMApiException(message + ": " + e.getMessage(), e);
        } catch (IOException e) {
            String message = "Failed to read truststore received from Cloudera Manager";
            LOGGER.error(message, e);
            throw new OnPremCMApiException(message);
        }
    }

    protected String getRootCertificateFromFile(File pem) throws IOException {
        return Files.readString(pem.toPath());
    }
}
