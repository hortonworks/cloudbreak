package com.sequenceiq.cloudbreak.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.KubernetesConfigRepository;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesConfigServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private KubernetesConfigService underTest;

    @Mock
    private KubernetesConfigRepository kubernetesConfigRepository;

    @Mock
    private SecretService secretService;

    @Before
    public void setup() {
        doAnswer(invocation -> invocation.getArgument(0)).when(kubernetesConfigRepository).save(any());
    }

    @Test
    public void testUpdateByWorkspaceIdNotFound() {
        thrown.expect(NotFoundException.class);

        underTest.updateByWorkspaceId(1L, new KubernetesConfig());
    }

    @Test
    public void testUpdateByWorkspaceIdOk() {
        KubernetesConfig originalConfig = new KubernetesConfig();
        originalConfig.setId(1L);
        originalConfig.setWorkspace(new Workspace());
        originalConfig.setEnvironments(Set.of(new EnvironmentView()));
        when(kubernetesConfigRepository.findByNameAndWorkspaceId(any(), anyLong())).thenReturn(Optional.of(originalConfig));

        KubernetesConfig result = underTest.updateByWorkspaceId(1L, new KubernetesConfig());

        verify(secretService, times(1)).delete(any());

        Assert.assertEquals(originalConfig.getId(), result.getId());
        Assert.assertEquals(originalConfig.getWorkspace(), result.getWorkspace());
        Assert.assertEquals(originalConfig.getEnvironments(), result.getEnvironments());
    }
}
