package com.sequenceiq.it.cloudbreak.newway.testcase.e2e.gcp;

import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;
import static com.sequenceiq.it.cloudbreak.newway.dto.InstanceGroupTestDto.withHostGroup;

import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.EncryptionType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.e2e.AbstractE2ETest;

public class GcpEncryptionTests extends AbstractE2ETest {

    public static final String KEY = "helloworld";

    public static final String HOSTGROUP_WORKER = "worker";

    public static final int DESIRED_COUNT_THREE = 3;

    public static final String PARAMETRIZED_CONTEXT = "parametrizedContext";

    @Inject
    private StackTestClient stackTestClient;

    @DataProvider(name = PARAMETRIZED_CONTEXT)
    public Object[][] testContextWithParameters(ITestContext iTestContext) {
        iTestContext.getSuite().getXmlSuite().getAllParameters();
        return new Object[][]{
                {getBean(TestContext.class), HOSTGROUP_WORKER, KeyEncryptionMethod.RAW, DESIRED_COUNT_THREE}};
    }

    @Test(dataProvider = PARAMETRIZED_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent where a given disks are encrypted in given hostgroup",
            then = "crreate is successful, the stack is stopable, startable, and scalable")
    public void testGcpClusterWithEncryptedDisks(TestContext onTestContext, String hostgroupName, KeyEncryptionMethod keyEncryptionMethod,
            int desiredCountToScale) {
        test(givenStackWithEncryptedDiskInHostgroup(hostgroupName, keyEncryptionMethod)
                        .andThen(stopAndWaitForIt())
                        .andThen(startAndWaitForIt())
                        .andThen(scaleAndWaitForIt(hostgroupName, desiredCountToScale)),
                onTestContext);
    }

    private void test(Function<TestContext, StackTestDto> function, TestContext testContext) {
        function.apply(testContext).validate();
    }

    Function<TestContext, StackTestDto> givenStackWithEncryptedDiskInHostgroup(String hostgroupName, KeyEncryptionMethod keyEncryptionMethod) {
        return testContext -> {
            List<InstanceGroupTestDto> instanceGroups = withHostGroup(testContext, MASTER, COMPUTE, WORKER);

            GcpInstanceTemplateV4Parameters params = new GcpInstanceTemplateV4Parameters();
            GcpEncryptionV4Parameters encryptionParams = new GcpEncryptionV4Parameters();
            encryptionParams.setKeyEncryptionMethod(keyEncryptionMethod);
            encryptionParams.setKey(KEY);
            encryptionParams.setType(EncryptionType.CUSTOM);
            params.setEncryption(encryptionParams);
            for (int i = 0; i < instanceGroups.size(); i++) {
                if (instanceGroups.get(i).getRequest().getName().equals(hostgroupName)) {
                    instanceGroups.get(i).getRequest().getTemplate().setGcp(params);
                }
            }
            return testContext.given(StackTestDto.class).withInstanceGroupsEntity(instanceGroups)
                    .when(stackTestClient.createV4())
                    .await(STACK_AVAILABLE);
        };
    }

    Function<StackTestDto, StackTestDto> scaleAndWaitForIt(String hostgroupName, int desiredCount) {
        return stack -> stack.when(stackTestClient.scalePostV4().withGroup(hostgroupName)
                .withDesiredCount(desiredCount))
                .await(STACK_AVAILABLE);
    }

    Function<StackTestDto, StackTestDto> stopAndWaitForIt() {
        return stack -> stack.when(stackTestClient.stopV4())
                .await(STACK_STOPPED);
    }

    Function<StackTestDto, StackTestDto> startAndWaitForIt() {
        return stack -> stack.when(stackTestClient.startV4())
                .await(STACK_AVAILABLE);
    }

    Function<StackTestDto, StackTestDto> delete() {
        return stack ->
                stack.then((tc, testDto, cc) -> stackTestClient.deleteV4().action(tc, testDto, cc));
    }
}

