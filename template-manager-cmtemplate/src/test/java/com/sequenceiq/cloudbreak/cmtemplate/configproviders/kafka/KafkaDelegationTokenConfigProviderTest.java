package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@ExtendWith(MockitoExtension.class)
class KafkaDelegationTokenConfigProviderTest {

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @InjectMocks
    private KafkaDelegationTokenConfigProvider underTest;

    @Test
    void testIsConfigurationNeededWhenNoKafkaBrokerRoleType() {
        KerberosConfig kerberosConfig = KerberosConfig
                .KerberosConfigBuilder
                .aKerberosConfig()
                .build();
        TemplatePreparationObject templatePreparationObject = TemplatePreparationObject.Builder
                .builder()
                .withKerberosConfig(kerberosConfig)
                .build();
        when(cmTemplateProcessor.isRoleTypePresentInService(underTest.getServiceType(), underTest.getRoleTypes())).thenReturn(Boolean.FALSE);

        boolean result = underTest.isConfigurationNeeded(cmTemplateProcessor, templatePreparationObject);

        Assertions.assertFalse(result);
    }

    @Test
    void testIsConfigurationNeededWhenKerberosIsNotEnabled() {
        TemplatePreparationObject templatePreparationObject = TemplatePreparationObject.Builder.builder().build();

        boolean result = underTest.isConfigurationNeeded(null, templatePreparationObject);

        Assertions.assertFalse(result);
    }

    @Test
    void testIsConfigurationNeededShouldReturnTrueWhenKafkaBrokerRoleTypeAndKafkaServiceAreAvailableAndKerberosIsEnabled() {
        KerberosConfig kerberosConfig = KerberosConfig
                .KerberosConfigBuilder
                .aKerberosConfig()
                .build();
        TemplatePreparationObject templatePreparationObject = TemplatePreparationObject.Builder
                .builder()
                .withKerberosConfig(kerberosConfig)
                .build();
        when(cmTemplateProcessor.isRoleTypePresentInService(underTest.getServiceType(), underTest.getRoleTypes())).thenReturn(Boolean.TRUE);

        boolean result = underTest.isConfigurationNeeded(cmTemplateProcessor, templatePreparationObject);

        Assertions.assertTrue(result);
    }
}