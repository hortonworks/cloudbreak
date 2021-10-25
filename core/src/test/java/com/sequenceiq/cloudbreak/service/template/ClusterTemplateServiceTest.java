package com.sequenceiq.cloudbreak.service.template;

import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
public class ClusterTemplateServiceTest {

    @InjectMocks
    private ClusterTemplateService underTest;

    @Mock
    private ClusterTemplateRepository clusterTemplateRepository;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private Workspace workspace;

    @Test
    public void testCheckDuplicationWhenExistedResourceEnvNullWithoutException() {
        String name = "new-name";
        ClusterTemplate newOne = new ClusterTemplate();
        newOne.setName(name);
        newOne.setStatus(ResourceStatus.USER_MANAGED);
        newOne.setWorkspace(workspace);
        Stack stack = new Stack();
        stack.setEnvironmentCrn("envCrn");
        newOne.setStackTemplate(stack);

        ClusterTemplate existed = new ClusterTemplate();
        existed.setStackTemplate(new Stack());

        when(clusterTemplateRepository.findByNameAndWorkspace(name, workspace)).thenReturn(Optional.of(existed));

        underTest.checkDuplication(newOne);
    }

    @Test
    public void testCheckDuplicationWhenNewResourceEnvNullWithoutException() {
        String name = "new-name";
        ClusterTemplate newOne = new ClusterTemplate();
        newOne.setName(name);
        newOne.setStatus(ResourceStatus.USER_MANAGED);
        newOne.setWorkspace(workspace);
        Stack stack = new Stack();
        newOne.setStackTemplate(stack);

        ClusterTemplate existed = new ClusterTemplate();
        Stack existedStack = new Stack();
        existedStack.setEnvironmentCrn("envCrn");
        existed.setStackTemplate(existedStack);

        when(clusterTemplateRepository.findByNameAndWorkspace(name, workspace)).thenReturn(Optional.of(existed));

        underTest.checkDuplication(newOne);
    }

    @Test
    public void testCheckDuplicationWhenEnvCrnisSame() {
        String name = "new-name";
        ClusterTemplate newOne = new ClusterTemplate();
        newOne.setName(name);
        newOne.setStatus(ResourceStatus.USER_MANAGED);
        newOne.setWorkspace(workspace);
        Stack stack = new Stack();
        stack.setEnvironmentCrn("envCrn");
        newOne.setStackTemplate(stack);

        ClusterTemplate existed = new ClusterTemplate();
        Stack existedStack = new Stack();
        existedStack.setEnvironmentCrn("envCrn");
        existed.setStackTemplate(existedStack);

        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setName("envName");

        when(clusterTemplateRepository.findByNameAndWorkspace(name, workspace)).thenReturn(Optional.of(existed));
        when(environmentClientService.getByCrn("envCrn")).thenReturn(envResponse);

        DuplicateClusterTemplateException actual = Assertions.assertThrows(DuplicateClusterTemplateException.class, () -> underTest.checkDuplication(newOne));
        Assertions.assertEquals("Cluster definition already exists with name 'new-name' for the environment of 'envName'", actual.getMessage());
    }

    @Test
    public void testCheckDuplicationWhenExistedResourceEnvAndNewOneenvCrnNull() {
        String name = "new-name";
        ClusterTemplate newOne = new ClusterTemplate();
        newOne.setName(name);
        newOne.setStatus(ResourceStatus.USER_MANAGED);
        newOne.setWorkspace(workspace);
        Stack stack = new Stack();
        newOne.setStackTemplate(stack);

        ClusterTemplate existed = new ClusterTemplate();
        existed.setStackTemplate(new Stack());

        when(clusterTemplateRepository.findByNameAndWorkspace(name, workspace)).thenReturn(Optional.of(existed));

        DuplicateClusterTemplateException actual = Assertions.assertThrows(DuplicateClusterTemplateException.class, () -> underTest.checkDuplication(newOne));
        Assertions.assertEquals("The default cluster definition could not be created with the same name", actual.getMessage());
    }
}
