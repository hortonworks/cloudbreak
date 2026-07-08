package com.sequenceiq.cloudbreak.controller.validation.stack;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.dataviz.DatavizRoles;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@ExtendWith(MockitoExtension.class)
class StackBlueprintValidatorTest {

    private static final String BLUEPRINT_TEXT = "blueprint-text";

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @InjectMocks
    private StackBlueprintValidator underTest;

    @Test
    void testValidateComponentsByRuntimeWhenDatavizIsPresentAndVersionIsValid() {
        StackDtoDelegate stack = mock(StackDtoDelegate.class);
        StackV4Request stackV4Request = mock(StackV4Request.class);
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        ServiceComponent serviceComponent = mock(ServiceComponent.class);

        Image image = createImage("7.3.2-1.cdh7.3.2.p100.79687996");

        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprintService.get(1L)).thenReturn(blueprint);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(serviceComponent.getService()).thenReturn(DatavizRoles.DATAVIZ);
        when(cmTemplateProcessor.getAllComponents()).thenReturn(Set.of(serviceComponent));

        assertDoesNotThrow(() -> underTest.validateComponentsByRuntime(stackV4Request, stack, image));
    }

    @Test
    void testValidateComponentsByRuntimeWhenDatavizIsPresentAndVersionIsInvalid() {
        StackDtoDelegate stack = mock(StackDtoDelegate.class);
        StackV4Request stackV4Request = mock(StackV4Request.class);
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        ServiceComponent serviceComponent = mock(ServiceComponent.class);

        Image image = createImage("7.2.0");

        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprintService.get(1L)).thenReturn(blueprint);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(serviceComponent.getService()).thenReturn(DatavizRoles.DATAVIZ);
        when(cmTemplateProcessor.getAllComponents()).thenReturn(Set.of(serviceComponent));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateComponentsByRuntime(stackV4Request, stack, image));
        assertEquals("DATAVIZ clusters only supported if Cloudera Manager is >= 7.3.2.100", exception.getMessage());
    }

    @Test
    void testValidateComponentsByRuntimeWhenDatavizIsNotPresent() {
        StackDtoDelegate stack = mock(StackDtoDelegate.class);
        StackV4Request stackV4Request = mock(StackV4Request.class);
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setBlueprintText(BLUEPRINT_TEXT);

        Image image = createImage("7.2.0");

        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprintService.get(1L)).thenReturn(blueprint);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getAllComponents()).thenReturn(Set.of());

        assertDoesNotThrow(() -> underTest.validateComponentsByRuntime(stackV4Request, stack, image));
    }

    @Test
    void testValidateComponentsByRuntimeWhenNoImageStackDetailsUsesProductVersion() {
        StackDto stack = mock(StackDto.class);
        StackV4Request stackV4Request = mock(StackV4Request.class);
        ClouderaManagerV4Request clouderaManagerV4Request = mock(ClouderaManagerV4Request.class);
        ClusterV4Request clusterV4Request = mock(ClusterV4Request.class);
        Image image = mock(Image.class);
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        ServiceComponent serviceComponent = mock(ServiceComponent.class);

        ClouderaManagerProductV4Request cdhProduct = new ClouderaManagerProductV4Request();
        cdhProduct.setName("CDH");
        cdhProduct.setVersion("7.3.2-1.cdh7.3.2.p100.12345678");

        when(image.getStackDetails()).thenReturn(null);
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(clusterV4Request.getCm()).thenReturn(clouderaManagerV4Request);
        when(clouderaManagerV4Request.getProducts()).thenReturn(List.of(cdhProduct));
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprintService.get(1L)).thenReturn(blueprint);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(serviceComponent.getService()).thenReturn(DatavizRoles.DATAVIZ);
        when(cmTemplateProcessor.getAllComponents()).thenReturn(Set.of(serviceComponent));

        assertDoesNotThrow(() -> underTest.validateComponentsByRuntime(stackV4Request, stack, image));
    }

    private Image createImage(String version) {
        StackRepoDetails stackRepoDetails = new StackRepoDetails(
                Map.of(com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPOSITORY_VERSION, version), null);
        ImageStackDetails imageStackDetails = new ImageStackDetails(version, stackRepoDetails, null);
        Image image = mock(Image.class);
        when(image.getStackDetails()).thenReturn(imageStackDetails);
        return image;
    }
}
