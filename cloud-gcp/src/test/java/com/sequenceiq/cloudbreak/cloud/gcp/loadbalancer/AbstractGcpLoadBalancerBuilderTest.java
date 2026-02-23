package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.compute.ComputeRequest;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AbstractGcpLoadBalancerBuilderTest {

    private static final Long STACK_ID = 1L;

    private static final String RESOURCE_NAME = "test-resource";

    private static final ResourceType RESOURCE_TYPE = ResourceType.GCP_FORWARDING_RULE;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private GcpResourceNameService resourceNameService;

    @Mock
    private ComputeRequest<Operation> mockComputeRequest;

    @InjectMocks
    private TestGcpLoadBalancerBuilder underTest;

    @Test
    void testConvertProtocolWithTcpFallback() {
        assertEquals("TCP", underTest.convertProtocolWithTcpFallback(NetworkProtocol.TCP));
        assertEquals("UDP", underTest.convertProtocolWithTcpFallback(NetworkProtocol.UDP));
        assertEquals("TCP", underTest.convertProtocolWithTcpFallback(null));
    }

    @Test
    void testEnrichParametersWithAttributes() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("paramKey", "paramValue");

        Map<String, Object> result = underTest.enrichParametersWithAttributes(parameters, LoadBalancerType.PUBLIC);

        assertNotNull(result);
        assertTrue(result.containsKey(CloudResource.ATTRIBUTES));
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = (Map<String, Object>) result.get(CloudResource.ATTRIBUTES);

        // Check that attributes from LoadBalancerTypeAttribute are present
        assertEquals(LoadBalancerTypeAttribute.class, attributes.get("attributeType"));
        assertEquals(LoadBalancerType.PUBLIC.name(), attributes.get("name"));

        // Check that original parameters are also merged into attributes
        assertEquals("paramValue", attributes.get("paramKey"));
    }

    @Test
    void testFetchResourceFromDbFindsCreated() {
        CloudResource createdResource = CloudResource.builder()
                .withType(RESOURCE_TYPE)
                .withStatus(CommonStatus.CREATED)
                .withName("created")
                .build();
        when(resourceRetriever.findByStatusAndTypeAndStack(CommonStatus.CREATED, RESOURCE_TYPE, STACK_ID))
                .thenReturn(Optional.of(createdResource));

        Optional<CloudResource> result = underTest.fetchResourceFromDb(RESOURCE_TYPE, STACK_ID);

        assertTrue(result.isPresent());
        assertEquals("created", result.get().getName());
    }

    @Test
    void testFetchResourceFromDbFindsRequestedWhenCreatedIsMissing() {
        CloudResource requestedResource = CloudResource.builder()
                .withType(RESOURCE_TYPE)
                .withStatus(CommonStatus.REQUESTED)
                .withName("requested")
                .build();
        when(resourceRetriever.findByStatusAndTypeAndStack(CommonStatus.CREATED, RESOURCE_TYPE, STACK_ID))
                .thenReturn(Optional.empty());
        when(resourceRetriever.findByStatusAndTypeAndStack(CommonStatus.REQUESTED, RESOURCE_TYPE, STACK_ID))
                .thenReturn(Optional.of(requestedResource));

        Optional<CloudResource> result = underTest.fetchResourceFromDb(RESOURCE_TYPE, STACK_ID);

        assertTrue(result.isPresent());
        assertEquals("requested", result.get().getName());
    }

    @Test
    void testFetchAllResourceFromDb() {
        CloudResource createdResource = CloudResource.builder()
                .withType(RESOURCE_TYPE)
                .withStatus(CommonStatus.CREATED)
                .withName("created")
                .build();
        CloudResource requestedResource = CloudResource.builder()
                .withType(RESOURCE_TYPE)
                .withStatus(CommonStatus.REQUESTED)
                .withName("requested")
                .build();

        when(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, RESOURCE_TYPE, STACK_ID))
                .thenReturn(new ArrayList<>(List.of(createdResource)));
        when(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.REQUESTED, RESOURCE_TYPE, STACK_ID))
                .thenReturn(new ArrayList<>(List.of(requestedResource)));

        List<CloudResource> result = underTest.fetchAllResourceFromDb(RESOURCE_TYPE, STACK_ID);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(r -> r.getName().equals("created")));
        assertTrue(result.stream().anyMatch(r -> r.getName().equals("requested")));
    }

    @Test
    void testMapPortToPortPart() {
        assertEquals("-80-", underTest.mapPortToPortPart(80));
    }

    @Test
    void testMapProtocolToPart() {
        when(resourceNameService.normalize("TCP")).thenReturn("tcp");
        assertEquals("-tcp-", underTest.mapProtocolToPart(NetworkProtocol.TCP));
    }

    @Test
    void testDoOperationalRequestSuccess() throws IOException {
        CloudResource resource = CloudResource.builder()
                .withType(RESOURCE_TYPE)
                .withStatus(CommonStatus.CREATED)
                .withName(RESOURCE_NAME)
                .build();
        Operation successOperation = new Operation().setName("op-name").setStatus("DONE");

        when(mockComputeRequest.execute()).thenReturn(successOperation);

        CloudResource result = underTest.doOperationalRequest(resource, mockComputeRequest);

        assertNotNull(result);
        assertEquals(RESOURCE_NAME, result.getName());
    }

    @Test
    void testDoOperationalRequestWithGcpErrorStatus() throws IOException {
        CloudResource resource = CloudResource.builder()
                .withType(RESOURCE_TYPE)
                .withStatus(CommonStatus.CREATED)
                .withName(RESOURCE_NAME)
                .build();
        Operation errorOperation = new Operation()
                .setHttpErrorStatusCode(404)
                .setHttpErrorMessage("Not Found");

        when(mockComputeRequest.execute()).thenReturn(errorOperation);

        GcpResourceException exception = assertThrows(GcpResourceException.class,
                () -> underTest.doOperationalRequest(resource, mockComputeRequest));
        assertEquals("Not Found: [ resourceType: GCP_FORWARDING_RULE,  resourceName: test-resource ]", exception.getMessage());
    }

    @Test
    void testDoOperationalRequestWithGoogleJsonResponseException() throws IOException {
        CloudResource resource = CloudResource.builder()
                .withType(RESOURCE_TYPE)
                .withStatus(CommonStatus.CREATED)
                .withName(RESOURCE_NAME)
                .build();
        GoogleJsonError error = new GoogleJsonError();
        error.setMessage("API Error");
        GoogleJsonResponseException gjrException = new GoogleJsonResponseException(
                new HttpResponseException.Builder(400, "Bad Request", new HttpHeaders()), error);

        when(mockComputeRequest.execute()).thenThrow(gjrException);

        GcpResourceException exception = assertThrows(GcpResourceException.class,
                () -> underTest.doOperationalRequest(resource, mockComputeRequest));
        assertTrue(exception.getMessage().contains("API Error"));
    }

    /**
     * A concrete implementation of the abstract class for testing purposes.
     */
    private static class TestGcpLoadBalancerBuilder extends AbstractGcpLoadBalancerBuilder {
        @Override
        public List<CloudResource> create(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer, Network network) {
            return Collections.emptyList();
        }

        @Override
        public List<CloudResource> build(GcpContext context, AuthenticatedContext auth, List<CloudResource> buildableResources, CloudLoadBalancer loadBalancer,
                CloudStack cloudStack) throws Exception {
            return List.of();
        }

        @Override
        public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
            return resource;
        }

        @Override
        public ResourceType resourceType() {
            return RESOURCE_TYPE;
        }

        @Override
        public int order() {
            return 0;
        }
    }
}
