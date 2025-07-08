package com.sequenceiq.freeipa.service.crossrealm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@ExtendWith(MockitoExtension.class)
class ActiveDirectoryCommandsBuilderTest {
    @Mock
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @InjectMocks
    private ActiveDirectoryCommandsBuilder underTest;

    @BeforeEach
    void setup() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(underTest, "freemarkerConfiguration", configuration);
    }

    @Test
    void testBuildCommandsWithLoadBalancer() throws IOException {
        // GIVEN
        Stack stack = new Stack();
        stack.setId(1L);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("freeipa.org");
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setTrustSecret("trustSecret");
        crossRealmTrust.setRealm("ad.org");
        crossRealmTrust.setFqdn("adHostName.ad.org");
        LoadBalancer loadBalancer = new LoadBalancer();
        Set<String> lbIps = new LinkedHashSet<>();
        Collections.addAll(lbIps, "ipaIp1", "ipaIp2", "ipaIp3");
        loadBalancer.setIp(lbIps);
        when(freeIpaLoadBalancerService.findByStackId(stack.getId())).thenReturn(Optional.of(loadBalancer));
        // WHEN
        String result = underTest.buildCommands(stack, freeIpa, crossRealmTrust);
        // THEN
        String expectedOutput = FileReaderUtils.readFileFromClasspath("crossrealmtrust/activedirectory_commands_lb.bat");
        assertEquals(expectedOutput, result);
    }

    @Test
    void testBuildCommandsWithoutLoadBalancer() throws IOException {
        // GIVEN
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setPrivateIp("ip1");
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setPrivateIp("ip2");
        Set<InstanceMetaData> instanceMetadatas = new LinkedHashSet<>();
        Collections.addAll(instanceMetadatas, instanceMetaData1, instanceMetaData2);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetadatas);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("freeipa.org");
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setTrustSecret("trustSecret");
        crossRealmTrust.setRealm("ad.org");
        crossRealmTrust.setFqdn("adHostName.ad.org");
        // WHEN
        String result = underTest.buildCommands(stack, freeIpa, crossRealmTrust);
        // THEN
        String expectedOutput = FileReaderUtils.readFileFromClasspath("crossrealmtrust/activedirectory_commands_nolb.bat");
        assertEquals(expectedOutput, result);
    }
}
