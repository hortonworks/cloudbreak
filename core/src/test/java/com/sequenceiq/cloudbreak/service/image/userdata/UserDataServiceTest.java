package com.sequenceiq.cloudbreak.service.image.userdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class UserDataServiceTest {

    private static final String TEST_RESOURCE_ID = "aa8997d3-527d-4e7f-af8a-7f7cd10eb8f7";

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:datahub:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:cluster:%s", TEST_RESOURCE_ID);

    private static final long STACK_ID = 1L;

    @InjectMocks
    private UserDataService underTest;

    @Mock
    private ImageService imageService;

    @Mock
    private StackService stackService;

    @Test
    void updateJumpgateFlagOnly() throws CloudbreakImageNotFoundException {
        Image image = new Image("alma", Map.of(InstanceGroupType.GATEWAY, ""), "", "", "", "", "", null);
        image.setUserdata(Map.of(InstanceGroupType.GATEWAY,
                "FLAG=foo\nIS_CCM_ENABLED=true\nIS_CCM_V2_ENABLED=false\nIS_CCM_V2_JUMPGATE_ENABLED=false\nOTHER_FLAG=bar"));
        when(imageService.getImage(STACK_ID)).thenReturn(image);
        when(stackService.getByIdWithLists(1L)).thenReturn(stack());

        underTest.updateJumpgateFlagOnly(1L);

        ArgumentCaptor<Map> imageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(imageService, times(1)).decorateImageWithUserDataForStack(any(), imageCaptor.capture());
        assertThat(imageCaptor.getValue().get(InstanceGroupType.GATEWAY))
                .isEqualTo("FLAG=foo\nIS_CCM_ENABLED=false\nIS_CCM_V2_ENABLED=true\nIS_CCM_V2_JUMPGATE_ENABLED=true\nOTHER_FLAG=bar");
    }

    private Stack stack() {
        Stack aStack = new Stack();
        aStack.setId(100L);
        aStack.setCluster(new Cluster());
        aStack.setResourceCrn(TEST_CLUSTER_CRN);
        aStack.setClusterNameAsSubdomain(false);
        return aStack;
    }
}
