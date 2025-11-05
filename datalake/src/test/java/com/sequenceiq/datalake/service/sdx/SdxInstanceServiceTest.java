package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.sdx.api.model.SdxClusterShape.MICRO_DUTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;

@ExtendWith(MockitoExtension.class)
class SdxInstanceServiceTest {
    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private SdxInstanceService underTest;

    @Test
    void testOverrideDefaultInstanceTypeWithCustomInstanceGroup() throws Exception {
        final String runtime = "7.2.12";
        String microDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/micro_duty.json");
        List<SdxInstanceGroupRequest> customInstanceGroups = List.of(withInstanceGroup("master", "verylarge"),
                withInstanceGroup("idbroker", "notverylarge"));
        StackV4Request stackV4Request = JsonUtil.readValue(microDutyJson, StackV4Request.class);

        underTest.overrideDefaultInstanceType(stackV4Request, customInstanceGroups, Collections.emptyList(),
                Collections.emptyList(), MICRO_DUTY);

        Optional<InstanceGroupV4Request> masterGroup = stackV4Request.getInstanceGroups()
                .stream()
                .filter(instanceGroup -> "master".equals(instanceGroup.getName()))
                .findAny();
        assertTrue(masterGroup.isPresent());
        assertEquals("verylarge", masterGroup.get().getTemplate().getInstanceType());
        Optional<InstanceGroupV4Request> idbrokerGroup = stackV4Request.getInstanceGroups()
                .stream()
                .filter(instanceGroup -> "idbroker".equals(instanceGroup.getName()))
                .findAny();
        assertTrue(idbrokerGroup.isPresent());
        assertEquals("notverylarge", idbrokerGroup.get().getTemplate().getInstanceType());
    }

    private SdxInstanceGroupRequest withInstanceGroup(String name, String instanceType) {
        SdxInstanceGroupRequest masterInstanceGroup = new SdxInstanceGroupRequest();
        masterInstanceGroup.setName(name);
        masterInstanceGroup.setInstanceType(instanceType);
        return masterInstanceGroup;
    }
}