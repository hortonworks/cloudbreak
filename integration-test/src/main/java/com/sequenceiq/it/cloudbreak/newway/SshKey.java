package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeyResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeysResponse;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.SshKeyV3Action;

public class SshKey extends Entity {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshKey.class);

    private static final String SSHKEY = "SSHKEY";

    private PlatformResourceRequestJson request = new PlatformResourceRequestJson();

    private PlatformSshKeysResponse response;

    private SshKey(String id) {
        super(id);
    }

    private SshKey() {
        this(SSHKEY);
    }

    public void setRequest(PlatformResourceRequestJson request) {
        this.request = request;
    }

    private Map<String, Set<PlatformSshKeyResponse>>  getResponseWithSshKeys() {
        return response.getSshKeys();
    }

    public PlatformResourceRequestJson getRequest() {
        return request;
    }

    public void setResponse(PlatformSshKeysResponse response) {
        this.response = response;
    }

    public SshKey withAvailabilityZone(String availabilityZone) {
        request.setAvailabilityZone(availabilityZone);
        return this;
    }

    public SshKey withCredentialId(Long id) {
        request.setCredentialId(id);
        return this;
    }

    public SshKey withCredentialName(String name) {
        request.setCredentialName(name);
        return this;
    }

    public SshKey withFilters(Map<String, String> filter) {
        request.setFilters(filter);
        return this;
    }

    public SshKey withPlatformVariant(String platformVariant) {
        request.setPlatformVariant(platformVariant);
        return this;
    }

    public SshKey withRegion(String region) {
        request.setRegion(region);
        return this;
    }

    static Function<IntegrationTestContext, SshKey> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, SshKey.class);
    }

    static Function<IntegrationTestContext, SshKey> getNew() {
        return testContext -> new SshKey();
    }

    public static SshKey request() {
        return new SshKey();
    }

    public static Action<SshKey> get(String key) {
        return new Action<>(getTestContext(key), SshKeyV3Action::post);
    }

    public static Action<SshKey> get() {
        return get(SSHKEY);
    }

    public static Assertion<SshKey> assertThis(BiConsumer<SshKey, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static Assertion<SshKey> assertValidSshKeys() {
        return assertThis((sshKey, t) -> {
            if (sshKey.getResponseWithSshKeys().isEmpty()) {
                LOGGER.info("No sshKeys for given provider");
            } else {
                for (Map.Entry<String, Set<PlatformSshKeyResponse>> elem : sshKey.getResponseWithSshKeys().entrySet()) {
                    for (Object response : elem.getValue()) {
                        PlatformSshKeyResponse platformSshKeyResponse = (PlatformSshKeyResponse) response;
                        Assert.assertFalse(platformSshKeyResponse.getName().isEmpty());
                    }
                }
            }
        });
    }
}