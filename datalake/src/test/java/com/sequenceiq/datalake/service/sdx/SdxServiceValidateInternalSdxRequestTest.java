package com.sequenceiq.datalake.service.sdx;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;

@ExtendWith(MockitoExtension.class)
public class SdxServiceValidateInternalSdxRequestTest {

    @InjectMocks
    private SdxService underTest;

    private SdxClusterRequest clusterRequest;

    private StackV4Request stackRequest;

    @BeforeEach
    void setUp() {
        clusterRequest = new SdxClusterRequest();
        stackRequest = null;
    }

    @Test
    void stackRequestNeedsCluster() {
        stackRequest = new StackV4Request();

        assertThatThrownBy(() -> underTest.validateInternalSdxRequest(stackRequest, clusterRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cluster cannot be null.");
    }

    @Test
    void customInstanceGroupWithoutStackRequest() {
        clusterRequest.setCustomInstanceGroups(List.of(new SdxInstanceGroupRequest()));

        assertThatCode(() -> underTest.validateInternalSdxRequest(stackRequest, clusterRequest))
                .doesNotThrowAnyException();
    }

    @Test
    void stackRequestWithCustomInstanceGroup() {
        stackRequest = new StackV4Request();
        stackRequest.setCluster(new ClusterV4Request());
        clusterRequest.setCustomInstanceGroups(List.of(new SdxInstanceGroupRequest()));

        assertThatThrownBy(() -> underTest.validateInternalSdxRequest(stackRequest, clusterRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Custom instance group is not accepted on SDX Internal API.");
    }

    @Test
    void customClusterShapeNeedStackRequest() {
        clusterRequest.setClusterShape(SdxClusterShape.CUSTOM);

        assertThatThrownBy(() -> underTest.validateInternalSdxRequest(stackRequest, clusterRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("CUSTOM cluster shape requires stack request.");
    }

    @Test
    void customClusterShapeWithStackRequest() {
        stackRequest = new StackV4Request();
        stackRequest.setCluster(new ClusterV4Request());
        clusterRequest.setClusterShape(SdxClusterShape.CUSTOM);

        assertThatCode(() -> underTest.validateInternalSdxRequest(stackRequest, clusterRequest))
                .doesNotThrowAnyException();
    }
}
