package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.common.type.InstanceGroupType.CORE;
import static com.sequenceiq.cloudbreak.common.type.InstanceGroupType.GATEWAY;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.Stack;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ArmFreemarkerConfiguration.class)
public class ArmTemplateBuilderTest {

    private final int volume1Size = 100;
    private final int volume2Size = 20;
    private final Long three = 3L;
    @InjectMocks
    private ArmTemplateBuilder underTest;
    private CloudStack cloudStack;
    private CloudContext cloudContext;
    private CloudCredential cloudCredential;

    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(underTest, "armTemplatePath", "templates/arm-v2.ftl");
        cloudStack = createCloudStack();
        cloudContext = createCloudContext();
        cloudCredential = createCloudCredential();
    }

    private CloudCredential createCloudCredential() {
        Map<String, Object> credentialMap = new HashMap<>();
        credentialMap.put("sshKey", "sshkey....");
        return new CloudCredential(1L, "testcredential", "pubkey", "cloudbreak", credentialMap);
    }

    private CloudStack createCloudStack() {
        Volume volume1 = new Volume("test1", "blob", volume1Size);
        Volume volume2 = new Volume("test2", "blob", volume2Size);

        InstanceTemplate instanceTemplate1 = new InstanceTemplate("Standard_A3", "cbgateway", 0L, asList(volume1, volume2),
                InstanceStatus.CREATE_REQUESTED, new HashMap<String, Object>());
        Group cbgateway = new Group("cbgateway", GATEWAY, asList(new CloudInstance("ID", instanceTemplate1)));

        InstanceTemplate instanceTemplate2 = new InstanceTemplate("Standard_A3", "master", 1L, asList(volume1), InstanceStatus.CREATE_REQUESTED,
                new HashMap<String, Object>());
        Group master = new Group("master", CORE, asList(new CloudInstance("ID", instanceTemplate2)));

        InstanceTemplate instanceTemplate3 = new InstanceTemplate("Standard_A3", "slave_1", 2L, asList(volume1), InstanceStatus.CREATE_REQUESTED,
                new HashMap<String, Object>());
        InstanceTemplate instanceTemplate4 = new InstanceTemplate("Standard_A3", "slave_1", three, asList(volume1), InstanceStatus.CREATE_REQUESTED,
                new HashMap<String, Object>());
        Group slave1 = new Group("slave_1", CORE, asList(new CloudInstance("ID", instanceTemplate3), new CloudInstance("ID", instanceTemplate4)));

        Network network = new Network(new Subnet("10.0.0.0/16"));

        List<SecurityRule> securityRuleList = new ArrayList<>();
        securityRuleList.add(new SecurityRule("10.0.0.0/16", new String[]{"80", "8080"}, "tcp"));
        Security security = new Security(securityRuleList);

        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, "CORE",
                InstanceGroupType.GATEWAY, "GATEWAY"
        );
        Image image = new Image("https://krisztian.blob.core.windows.net/images/cb-centos71-amb210-2015-07-22-b1470.vhd", userData);

        return new CloudStack(asList(cbgateway, master, slave1), network, security, image, new HashMap<String, String>());
    }

    private CloudContext createCloudContext() {
        Stack stack = TestUtil.stack();
        Location location = location(region("region"), availabilityZone("availabilityZone"));
        return new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform().name(), stack.getOwner(), stack.getPlatformVariant(), location);
    }

    @Ignore
    @Test
    public void test1() {
        String result = underTest.build("teststack1", cloudCredential, cloudContext, cloudStack);
    }


}
