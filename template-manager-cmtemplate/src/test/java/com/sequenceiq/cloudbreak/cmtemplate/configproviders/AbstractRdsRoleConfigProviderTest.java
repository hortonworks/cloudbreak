package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static com.sequenceiq.cloudbreak.TestUtil.rdsConfig;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

class AbstractRdsRoleConfigProviderTest {

    private AbstractRdsRoleConfigProvider subject = new AbstractRdsRoleConfigProvider() {
        @Override
        protected DatabaseType dbType() {
            return DatabaseType.RANGER;
        }

        @Override
        protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
            return List.of();
        }

        @Override
        public String getServiceType() {
            return "service";
        }

        @Override
        public List<String> getRoleTypes() {
            return List.of("role1", "role2");
        }
    };

    @Mock
    private CmTemplateProcessor templateProcessor;

    @Mock
    private TemplatePreparationObject source;

    @BeforeEach
    void setup() {
        initMocks(this);
    }

    @Test
    void configurationNeededIfRdsConfigAndRoleBothPresent() {
        RDSConfig rdsConfig = rdsConfig(DatabaseType.RANGER);
        when(source.getRdsConfig(DatabaseType.RANGER)).thenReturn(rdsConfig);
        when(templateProcessor.isRoleTypePresentInService(subject.getServiceType(), subject.getRoleTypes())).thenReturn(Boolean.TRUE);

        assertTrue(subject.isConfigurationNeeded(templateProcessor, source));
    }

    @Test
    void configurationNotNeededIfRoleAbsent() {
        RDSConfig rdsConfig = rdsConfig(DatabaseType.RANGER);
        when(source.getRdsConfig(DatabaseType.RANGER)).thenReturn(rdsConfig);
        when(templateProcessor.isRoleTypePresentInService(subject.getServiceType(), subject.getRoleTypes())).thenReturn(Boolean.FALSE);

        assertFalse(subject.isConfigurationNeeded(templateProcessor, source));
    }

    @Test
    void configurationNotNeededIfRdsConfigAbsent() {
        when(source.getRdsConfig(DatabaseType.RANGER)).thenReturn(null);
        when(templateProcessor.isRoleTypePresentInService(subject.getServiceType(), subject.getRoleTypes())).thenReturn(Boolean.TRUE);

        assertFalse(subject.isConfigurationNeeded(templateProcessor, source));
    }

}
