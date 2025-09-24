package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;

public class LdapClusterTest extends AbstractMockTest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid cluster request with ldap configuration",
            when = "calling create cluster",
            then = "the ldap should be configured on the cluster")
    public void testCreateClusterWithLdap(MockedTestContext testContext) {

        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .awaitForFlow()
                .enableVerification()
                .await(STACK_AVAILABLE)
                .mockCm().externalUserMappings().post().times(1).verify()
                .validate();
    }
}
