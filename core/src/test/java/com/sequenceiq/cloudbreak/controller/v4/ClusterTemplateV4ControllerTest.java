package com.sequenceiq.cloudbreak.controller.v4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Responses;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;

@ExtendWith(MockitoExtension.class)
class ClusterTemplateV4ControllerTest {

    private static final Long WORKSPACE_ID = 123L;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private ClusterTemplateService clusterTemplateService;

    @Mock
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @InjectMocks
    private ClusterTemplateV4Controller underTest;

    @Test
    void testList() {
        Set<ClusterTemplateViewV4Response> responses = Set.of();
        when(clusterTemplateService.listInWorkspaceAndCleanUpInvalids(WORKSPACE_ID)).thenReturn(responses);
        when(threadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);

        ClusterTemplateViewV4Responses result = underTest.list(WORKSPACE_ID);

        assertThat(result).isNotNull();
        assertThat(result.getResponses()).isSameAs(responses);
        verify(blueprintService).updateDefaultBlueprintCollection(WORKSPACE_ID);
        verify(clusterTemplateService).updateDefaultClusterTemplates(WORKSPACE_ID);
    }

}