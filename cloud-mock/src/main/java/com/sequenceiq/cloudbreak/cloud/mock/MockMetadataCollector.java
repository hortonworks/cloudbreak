package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Service
public class MockMetadataCollector implements MetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockMetadataCollector.class);

    @Inject
    private MockCredentialViewFactory mockCredentialViewFactory;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms,
            List<CloudInstance> knownInstances) {
        MockCredentialView mockCredentialView = mockCredentialViewFactory.createCredetialView(authenticatedContext.getCloudCredential());
        LOGGER.info("Collect metadata from mock spi, server address: " + mockCredentialView.getMockEndpoint());
        String url = mockCredentialView.getMockEndpoint() + "/spi/cloud_metadata_statuses";
        try {
            CloudVmMetaDataStatus[] response = Unirest.post(url)
                    .body(vms)
                    .asObject(CloudVmMetaDataStatus[].class).getBody();
            return Arrays.asList(response);
        } catch (UnirestException e) {
            LOGGER.error("Url invocation failed: " + url, e);
            throw new CloudbreakServiceException("can't convert to object", e);
        }
    }
}
