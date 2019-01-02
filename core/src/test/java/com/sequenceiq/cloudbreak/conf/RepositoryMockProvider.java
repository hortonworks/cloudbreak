package com.sequenceiq.cloudbreak.conf;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.repository.workspace.TenantRepository;
import com.sequenceiq.cloudbreak.repository.workspace.UserRepository;

@Configuration
@MockBean({UserRepository.class, TenantRepository.class})
public class RepositoryMockProvider {
}
