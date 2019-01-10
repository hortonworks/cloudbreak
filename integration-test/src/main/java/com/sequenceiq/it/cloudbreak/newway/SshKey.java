package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.requests.PlatformResourceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSshKeyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSshKeysV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.SshKeyV4Action;

public class SshKey extends Entity {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshKey.class);

    private static final String SSHKEY = "SSHKEY";

    private PlatformResourceV4Request request = new PlatformResourceV4Request();

    private PlatformSshKeysV4Response response;

    private SshKey(String id) {
        super(id);
    }

    private SshKey() {
        this(SSHKEY);
    }

    public void setRequest(PlatformResourceV4Request request) {
        this.request = request;
    }

    private Map<String, Set<PlatformSshKeyV4Response>>  getResponseWithSshKeys() {
        return response.getSshKeys();
    }

    public PlatformResourceV4Request getRequest() {
        return request;
    }

    public void setResponse(PlatformSshKeysV4Response response) {
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
        return new Action<>(getTestContext(key), SshKeyV4Action::post);
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
                for (Map.Entry<String, Set<PlatformSshKeyV4Response>> elem : sshKey.getResponseWithSshKeys().entrySet()) {
                    for (Object response : elem.getValue()) {
                        PlatformSshKeyV4Response platformSshKeyV4Response = (PlatformSshKeyV4Response) response;
                        Assert.assertFalse(platformSshKeyV4Response.getName().isEmpty());
                    }
                }
            }
        });
    }
}