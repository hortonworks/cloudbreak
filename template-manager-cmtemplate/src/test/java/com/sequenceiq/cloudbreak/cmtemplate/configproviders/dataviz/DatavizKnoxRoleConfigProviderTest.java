package com.sequenceiq.cloudbreak.cmtemplate.configproviders.dataviz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;

@ExtendWith(MockitoExtension.class)
class DatavizKnoxRoleConfigProviderTest {

    @InjectMocks
    private DatavizKnoxRoleConfigProvider underTest;

    @Test
    void testGetServiceConfigs() {
        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(null, null);
        assertEquals(1, serviceConfigs.size());
        assertEquals("AUTHENTICATION_BACKENDS", serviceConfigs.get(0).getName());
        assertEquals("arcwebbase.backends.KnoxSpnegoDjangoBackend", serviceConfigs.get(0).getValue());
    }

}