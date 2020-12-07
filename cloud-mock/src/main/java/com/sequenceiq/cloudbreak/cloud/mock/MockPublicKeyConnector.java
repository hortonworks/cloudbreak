package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyDescribeRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyRegisterRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyUnregisterRequest;

@Service
public class MockPublicKeyConnector implements PublicKeyConnector {

    @Inject
    private MockUrlFactory mockUrlFactory;

    @Override
    public void register(PublicKeyRegisterRequest request) {
        String json = new Gson().toJson(Map.of("publicKeyId", request.getPublicKeyId(), "publicKey", request.getPublicKey()));
        try (Response response = mockUrlFactory.get("/spi/register_public_key")
                .post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE))) {
            if (response.getStatus() != 200) {
                throw new CloudConnectorException(response.readEntity(String.class));
            }
        }
    }

    @Override
    public void unregister(PublicKeyUnregisterRequest request) {
        String json = new Gson().toJson(Map.of("publicKeyId", request.getPublicKeyId()));
        try (Response response = mockUrlFactory.get("/spi/unregister_public_key")
                .post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE))) {
            if (response.getStatus() != 200) {
                throw new CloudConnectorException(response.readEntity(String.class));
            }
        }
    }

    @Override
    public boolean exists(PublicKeyDescribeRequest request) {
        try (Response response = mockUrlFactory.get("/spi/get_public_key/" + request.getPublicKeyId()).get()) {
            if (response.getStatus() != 200) {
                throw new CloudConnectorException(response.readEntity(String.class));
            }
            Map<String, String> entity = response.readEntity(Map.class);
            return entity != null && !entity.isEmpty();
        }
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
