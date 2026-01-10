package com.sequenceiq.freeipa.service.crossrealm.commands.mit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@ExtendWith(MockitoExtension.class)
class MitBaseClusterKrb5ConfBuilderTest {
    @Mock
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @InjectMocks
    private MitBaseClusterKrb5ConfBuilder underTest;

    @BeforeEach
    void setup() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(underTest, "freemarkerConfiguration", configuration);
    }

    @ParameterizedTest
    @EnumSource(value = TrustCommandType.class, mode = EnumSource.Mode.EXCLUDE, names = "VALIDATION")
    void testBuildCommands(TrustCommandType trustCommandType) throws IOException {
        // GIVEN
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("freeipa.org");
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setTrustSecret("trustSecret");
        crossRealmTrust.setKdcRealm("ad.org");
        crossRealmTrust.setKdcFqdn("adHostName.ad.org");
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setFqdn("ipa.freeipa.org");
        // WHEN
        String result = underTest.buildCommands("resourceName", trustCommandType, freeIpa, crossRealmTrust, loadBalancer);
        // THEN
        String fileName = String.format("crossrealmtrust/basecluster/mit_basecluster_krb5conf_%s.sh", trustCommandType.name().toLowerCase());
        String expectedOutput = FileReaderUtils.readFileFromClasspath(fileName);
        assertEquals(expectedOutput, result);
    }
}
