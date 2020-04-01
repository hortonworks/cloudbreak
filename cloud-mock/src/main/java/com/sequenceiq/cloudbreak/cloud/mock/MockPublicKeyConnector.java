package com.sequenceiq.cloudbreak.cloud.mock;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyDescribeRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyRegisterRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyUnregisterRequest;

@Service
public class MockPublicKeyConnector implements PublicKeyConnector {

    @Inject
    private MockCredentialViewFactory mockCredentialViewFactory;

    @Override
    public void register(PublicKeyRegisterRequest request) {
        try {
            String mockEndpoint = getMockEndpoint(request.getCredential());
            HttpResponse<String> response = Unirest.post(mockEndpoint + "/spi/register_public_key").body(request.getPublicKeyId()).asString();
            if (response.getStatus() != 200) {
                throw new CloudConnectorException(response.getStatusText());
            }
        } catch (UnirestException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    @Override
    public void unregister(PublicKeyUnregisterRequest request) {
        try {
            String mockEndpoint = getMockEndpoint(request.getCredential());
            HttpResponse<String> response = Unirest.post(mockEndpoint + "/spi/unregister_public_key").body(request.getPublicKeyId()).asString();
            if (response.getStatus() != 200) {
                throw new CloudConnectorException(response.getStatusText());
            }
        } catch (UnirestException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(PublicKeyDescribeRequest request) {
        try {
            String mockEndpoint = getMockEndpoint(request.getCredential());
            HttpResponse<Boolean> response = Unirest.get(mockEndpoint + "/spi/get_public_key/" + request.getPublicKeyId()).asObject(Boolean.class);
            if (response.getStatus() != 200) {
                throw new CloudConnectorException(response.getStatusText());
            }
            return response.getBody() != null && response.getBody();
        } catch (UnirestException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private String getMockEndpoint(CloudCredential cloudCredential) {
        MockCredentialView mockCredentialView = mockCredentialViewFactory.createCredetialView(cloudCredential);
        return mockCredentialView.getMockEndpoint();
    }

    @Override
    public Platform platform() {
        return Platform.platform("MOCK");
    }

    @Override
    public Variant variant() {
        return Variant.variant("MOCK");
    }
}
