package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiParcel;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerProductTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.mock.SetupCmScalingMock;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.util.cleanup.ParcelGeneratorUtil;
import com.sequenceiq.it.util.cleanup.ParcelMockActivatorUtil;

public class ClouderaManagerDownscaleTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDownscaleTest.class);

    private static final BigDecimal DEPLOY_CLIENT_CONFIG_COMMAND_ID = new BigDecimal(100);

    private static final BigDecimal APPLY_HOST_TEMPLATE_COMMAND_ID = new BigDecimal(200);

    private static final BigDecimal HOSTS_DECOMMISSION_COMMAND_ID = new BigDecimal(300);

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private ParcelMockActivatorUtil parcelMockActivatorUtil;

    @Inject
    private ParcelGeneratorUtil parcelGeneratorUtil;

    @BeforeMethod
    public void setUp() {
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a stack with upscale",
            when = "upscale to 15 it downscale to 6",
            then = "stack is running")
    public void testDownscale(MockedTestContext testContext) {
        ApiParcel parcel = parcelGeneratorUtil.getActivatedCDHParcel();
        String clusterName = resourcePropertyProvider().getName();
        parcelMockActivatorUtil.mockActivateWithDefaultParcels(testContext, clusterName, parcel);
        SetupCmScalingMock mock = new SetupCmScalingMock();
        mock.configure(testContext, 3, 15, 6);
        testContext
                .given("cmpkey", ClouderaManagerProductTestDto.class)
                .withParcel("someParcel")
                .withName(parcel.getProduct())
                .withVersion(parcel.getVersion())
                .given("cmanager", ClouderaManagerTestDto.class)
                .withClouderaManagerProduct("cmpkey")
                .given("cmpclusterkey", ClusterTestDto.class)
                .withClouderaManager("cmanager")
                .given(StackTestDto.class)
                .withName(clusterName)
                .withCluster("cmpclusterkey")
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(mock.getDesiredWorkerCount()))
                .await(StackTestDto.class, STACK_AVAILABLE, 3000)
                .when(StackScalePostAction.valid().withDesiredCount(mock.getDesiredBackscaledWorkerCount()))
                .await(StackTestDto.class, STACK_AVAILABLE, 3000)
                .validate();
    }

}
