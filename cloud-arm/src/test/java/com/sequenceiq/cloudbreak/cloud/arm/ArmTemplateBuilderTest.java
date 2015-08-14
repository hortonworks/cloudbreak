package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.CORE;
import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.GATEWAY;

import java.util.ArrayList;
import java.util.Arrays;
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

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ArmFreemarkerConfiguration.class)
public class ArmTemplateBuilderTest {

    @InjectMocks
    private ArmTemplateBuilder underTest;

    private CloudStack cloudStack;
    private CloudCredential cloudCredential;
    private final int volume1Size = 100;
    private final int volume2Size = 20;
    private final int three = 3;

    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(underTest, "armTemplatePath", "templates/arm-v2.ftl");
        cloudStack = createCloudStack();
        cloudCredential = createCloudCredential();
    }

    private CloudCredential createCloudCredential() {
        Map<String, Object> credentialMap = new HashMap<>();
        credentialMap.put("sshKey", "sshkey....");
        return new CloudCredential("testcredential", credentialMap);
    }

    private CloudStack createCloudStack() {
        Volume volume1 = new Volume("test1", "blob", volume1Size);
        Volume volume2 = new Volume("test2", "blob", volume2Size);

        InstanceTemplate instanceTemplate1 = new InstanceTemplate("Standard_A3", "cbgateway", 0, Arrays.asList(volume1, volume2));
        Group cbgateway = new Group("cbgateway", GATEWAY, Arrays.asList(instanceTemplate1));

        InstanceTemplate instanceTemplate2 = new InstanceTemplate("Standard_A3", "master", 1, Arrays.asList(volume1));
        Group master = new Group("master", CORE, Arrays.asList(instanceTemplate2));

        InstanceTemplate instanceTemplate3 = new InstanceTemplate("Standard_A3", "slave_1", 2, Arrays.asList(volume1));
        InstanceTemplate instanceTemplate4 = new InstanceTemplate("Standard_A3", "slave_1", three, Arrays.asList(volume1));
        Group slave1 = new Group("slave_1", CORE, Arrays.asList(instanceTemplate3, instanceTemplate4));

        Network network = new Network(new Subnet("10.0.0.0/16"));

        List<SecurityRule> securityRuleList = new ArrayList<>();
        securityRuleList.add(new SecurityRule("10.0.0.0/16", new String[]{"80", "8080"}, "tcp"));
        Security security = new Security(securityRuleList);

        Image image = new Image("https://krisztian.blob.core.windows.net/images/cb-centos71-amb210-2015-07-22-b1470.vhd");

        return new CloudStack(Arrays.asList(cbgateway, master, slave1), network, security, image, "West US");
    }

    @Ignore
    @Test
    public void test1() {
        String result = underTest.build("teststack1", cloudCredential, cloudStack);
    }


}
