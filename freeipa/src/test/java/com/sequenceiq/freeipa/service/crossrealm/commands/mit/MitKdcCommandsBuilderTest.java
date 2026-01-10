package com.sequenceiq.freeipa.service.crossrealm.commands.mit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

import java.io.IOException;
import java.util.List;

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
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.StackHelper;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@ExtendWith(MockitoExtension.class)
class MitKdcCommandsBuilderTest {
    @Mock
    private StackHelper stackHelper;

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @InjectMocks
    private MitKdcCommandsBuilder underTest;

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
        Stack stack = new Stack();
        stack.setId(1L);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("freeipa.org");
        CrossRealmTrust crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setTrustSecret("trustsecret");
        crossRealmTrust.setKdcRealm("kdc.realm");
        lenient().when(stackHelper.getServerIps(stack)).thenReturn(List.of("ipaIp1", "ipaIp2", "ipaIp3"));
        // WHEN
        String result = underTest.buildCommands(trustCommandType, freeIpa, crossRealmTrust);
        // THEN
        String fileName = String.format("crossrealmtrust/mit/mit_kdc_%s_commands.sh", trustCommandType.name().toLowerCase());
        String expectedOutput = FileReaderUtils.readFileFromClasspath(fileName);
        assertEquals(expectedOutput, result);
    }
}
