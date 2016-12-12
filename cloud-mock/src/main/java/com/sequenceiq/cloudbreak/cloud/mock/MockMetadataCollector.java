package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

@Service
public class MockMetadataCollector implements MetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockMetadataCollector.class);

    @Value("${mock.spi.endpoint:https://localhost:9443}")
    private String mockServerAddress;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        try {
            LOGGER.info("collect metadata from mock spi, server address: " + mockServerAddress);
            CloudVmMetaDataStatus[] response = Unirest.post(mockServerAddress + "/spi/cloud_metadata_statuses")
                    .body(vms)
                    .asObject(CloudVmMetaDataStatus[].class).getBody();
            return Arrays.asList(response);
        } catch (UnirestException e) {
            throw new RuntimeException("can't convert to object", e);
        }
    }
}
