package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.FailurePolicyJson;
import com.sequenceiq.cloudbreak.controller.json.InstanceGroupJson;
import com.sequenceiq.cloudbreak.controller.json.StackResponse;
import com.sequenceiq.cloudbreak.domain.AwsNetwork;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Stack;

public class StackToJsonConverterTest extends AbstractEntityConverterTest<Stack> {

    @InjectMocks
    private StackToJsonConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        underTest = new StackToJsonConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new ClusterResponse())
                .willReturn(new FailurePolicyJson());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupJson>());
        // WHEN
        StackResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertWithoutCredential() {
        // GIVEN
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new ClusterResponse())
                .willReturn(new FailurePolicyJson());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupJson>());
        // WHEN
        StackResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("credentialId", "cloudPlatform"));
    }

    @Test
    public void testConvertWithoutCluster() {
        // GIVEN
        getSource().setCluster(null);
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new FailurePolicyJson());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupJson>());
        getSource().setCluster(null);
        // WHEN
        StackResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cluster"));
    }

    @Test
    public void testConvertWithoutFailurePolicy() {
        // GIVEN
        getSource().setFailurePolicy(null);
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new ClusterResponse());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupJson>());
        // WHEN
        StackResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("failurePolicy"));
    }

    @Test
    public void testConvertWithoutNetwork() {
        // GIVEN
        getSource().setNetwork(null);
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new ClusterResponse())
                .willReturn(new FailurePolicyJson());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupJson>());
        // WHEN
        StackResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("networkId"));
    }

    @Override
    public Stack createSource() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        Network network = new AwsNetwork();
        network.setId(1L);
        stack.setNetwork(network);
        stack.setFailurePolicy(new FailurePolicy());
        stack.setParameters(new HashMap<String, String>());
        return stack;
    }
}
