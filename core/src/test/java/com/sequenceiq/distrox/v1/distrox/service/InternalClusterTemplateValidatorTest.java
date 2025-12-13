package com.sequenceiq.distrox.v1.distrox.service;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;

@ExtendWith(MockitoExtension.class)
public class InternalClusterTemplateValidatorTest {

    @InjectMocks
    private InternalClusterTemplateValidator underTest;

    static Object[][] internalClusterTemplateValidatorData() {
        return new Object[][]{
                { FeatureState.INTERNAL, false, false },
                { FeatureState.INTERNAL, true, true },

                { FeatureState.RELEASED, false, true },
                { FeatureState.RELEASED, true, true },

                { FeatureState.PREVIEW, false, true },
                { FeatureState.PREVIEW, true, true },
        };
    }

    @ParameterizedTest(name = "state = {0} internalTenant = {1} expectedShouldPopulate = {2}")
    @MethodSource("internalClusterTemplateValidatorData")
    public void internalClusterTemplateValidatorDataWithClusterTemplateObject(
            FeatureState state, boolean internalTenant, boolean expectedShouldPopulate) {
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setFeatureState(state);
        boolean shouldPopulate = underTest.shouldPopulate(clusterTemplate, internalTenant);
        assertEquals(expectedShouldPopulate, shouldPopulate);
    }

    @ParameterizedTest(name = "state = {0} internalTenant = {1} expectedShouldPopulate = {2}")
    @MethodSource("internalClusterTemplateValidatorData")
    public void internalClusterTemplateValidatorDataWithDefaultClusterTemplateV4RequestObject(
            FeatureState state, boolean internalTenant, boolean expectedShouldPopulate) {
        DefaultClusterTemplateV4Request clusterTemplate = new DefaultClusterTemplateV4Request();
        clusterTemplate.setFeatureState(state);
        boolean shouldPopulate = underTest.shouldPopulate(clusterTemplate, internalTenant);
        assertEquals(expectedShouldPopulate, shouldPopulate);
    }

    @ParameterizedTest(name = "state = {0} internalTenant = {1} expectedShouldPopulate = {2}")
    @MethodSource("internalClusterTemplateValidatorData")
    public void internalClusterTemplateValidatorDataWithClusterTemplateViewObject(
            FeatureState state, boolean internalTenant, boolean expectedShouldPopulate) {
        ClusterTemplateView clusterTemplate = new ClusterTemplateView();
        clusterTemplate.setFeatureState(state);
        boolean shouldPopulate = underTest.shouldPopulate(clusterTemplate, internalTenant);
        assertEquals(expectedShouldPopulate, shouldPopulate);
    }

    @ParameterizedTest(name = "state = {0} internalTenant = {1} expectedShouldPopulate = {2}")
    @MethodSource("internalClusterTemplateValidatorData")
    public void internalClusterTemplateValidatorDataWithisInternalTemplateMethod(
            FeatureState state, boolean internalTenant, boolean expectedShouldPopulate) {
        boolean internalTemplateInNotInternalTenant = underTest.isInternalTemplateInNotInternalTenant(internalTenant, state);
        assertEquals(!expectedShouldPopulate, internalTemplateInNotInternalTenant);
    }
}