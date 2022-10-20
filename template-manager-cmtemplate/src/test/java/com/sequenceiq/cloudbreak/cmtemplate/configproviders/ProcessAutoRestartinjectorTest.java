package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@ExtendWith(MockitoExtension.class)
public class ProcessAutoRestartinjectorTest {

    private static final String SERVICE_TYPE = "ZEPPELIN";

    private static final String ROLE_TYPE = "hiveserver2";

    @Mock
    private TemplatePreparationObject source;

    @InjectMocks
    private ProcessAutoRestartInjector underTest;

    @Test
    public void testGetRoleConfigsWithServiceAndRoletype() {
        ApiClusterTemplateService service = new ApiClusterTemplateService();
        service.setServiceType(SERVICE_TYPE);
        ApiClusterTemplateRoleConfigGroup roleConfigGroup = new ApiClusterTemplateRoleConfigGroup();
        roleConfigGroup.setRoleType(ROLE_TYPE);
        ApiClusterTemplateConfig autoRestartConfig = config("process_auto_restart", Boolean.TRUE.toString());

        assertEquals(underTest.getRoleConfigs(roleConfigGroup, service, source), List.of(autoRestartConfig));

    }

    @Test
    public void testGetRoleConfigsServiceTypeIsNOTPresentThenShouldNOTThrowError() {
        ApiClusterTemplateService service = new ApiClusterTemplateService();
        ApiClusterTemplateRoleConfigGroup roleConfigGroup = new ApiClusterTemplateRoleConfigGroup();
        roleConfigGroup.setRoleType(ROLE_TYPE);

        assertDoesNotThrow(() -> underTest.getRoleConfigs(roleConfigGroup, service, source));
        assertEquals(underTest.getRoleConfigs(roleConfigGroup, service, source), List.of());
    }

    @Test
    public void testGetRoleConfigsRoleTypeIsNOTPresentThenShouldNOTThrowError() {
        ApiClusterTemplateService service = new ApiClusterTemplateService();
        service.setServiceType(SERVICE_TYPE);
        ApiClusterTemplateRoleConfigGroup roleConfigGroup = new ApiClusterTemplateRoleConfigGroup();

        assertDoesNotThrow(() -> underTest.getRoleConfigs(roleConfigGroup, service, source));
        assertEquals(underTest.getRoleConfigs(roleConfigGroup, service, source), List.of());
    }

    @Test
    public void testGetRoleConfigsShouldReturnEmptyListWhenRoleTypeIsGateway() {
        ApiClusterTemplateService service = new ApiClusterTemplateService();
        service.setServiceType(SERVICE_TYPE);
        ApiClusterTemplateRoleConfigGroup roleConfigGroup = new ApiClusterTemplateRoleConfigGroup();
        roleConfigGroup.setRoleType("GATEWAY");

        assertEquals(underTest.getRoleConfigs(roleConfigGroup, service, source), List.of());
    }

}
