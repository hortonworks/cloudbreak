package com.sequenceiq.cloudbreak.conf;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Configuration
@MockBean({WorkspaceService.class, UserService.class, CloudbreakRestRequestThreadLocalService.class, ExposedServiceListValidator.class})
public class ConverterMockProvider {

}
