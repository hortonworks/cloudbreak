package com.sequenceiq.it.cloudbreak.testcase.e2e;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class  BasicSdxTests extends AbstractE2ETest {

    protected static final SdxClusterStatusResponse SDX_RUNNING = SdxClusterStatusResponse.RUNNING;

    protected static final String IDBROKER_HOSTGROUP = HostGroupType.IDBROKER.getName();

    protected static final String MASTER_HOSTGROUP = HostGroupType.MASTER.getName();

    private Map<String, InstanceStatus> instancesRegistered = new HashMap<>() {{
        put(MASTER_HOSTGROUP, InstanceStatus.SERVICES_HEALTHY);
        put(IDBROKER_HOSTGROUP, InstanceStatus.SERVICES_HEALTHY);
    }};

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private WaitUtil waitUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a basic SDX create request is sent (with Network and FreeIPA only)",
            then = "SDX should be available AND deletable"
    )
    public void testBasicSDXCanBeCreatedThenDeletedSuccessfully(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        testContext
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.create(), key(sdx))
                .await(SDX_RUNNING)
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, instancesRegistered);
                })
                .validate();
    }

    protected SdxTestClient sdxTestClient() {
        return sdxTestClient;
    }

    protected Map<String, InstanceStatus> getSdxInstancesRegisteredState() {
        return instancesRegistered;
    }
}
