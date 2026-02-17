package com.sequenceiq.cloudbreak.service.validation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.parcel.ParcelFilterService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class ParcelValidationAndFilteringServiceTest {

    @Mock
    private ParcelFilterService parcelFilterService;

    @InjectMocks
    private ParcelValidationAndFilteringService underTest;

    @Test
    void testValidateWhenNotDatahub() {
        Stack stack = mock(Stack.class);
        when(stack.isDatahub()).thenReturn(false);
        ValidationResult.ValidationResultBuilder validationBuilder = mock(ValidationResult.ValidationResultBuilder.class);

        underTest.validate(stack, validationBuilder);

        verifyNoInteractions(parcelFilterService);
        verifyNoInteractions(validationBuilder);
    }

    @Test
    void testValidateWhenDatahubSuccess() {
        Stack stack = createStack();
        Blueprint blueprint = stack.getCluster().getBlueprint();

        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setName("product");
        Component component = createComponent(product);
        when(stack.getComponents()).thenReturn(Set.of(component));

        when(parcelFilterService.filterParcelsByBlueprint(eq(1L), eq(1L), anySet(), eq(blueprint))).thenReturn(Set.of(product));
        ValidationResult.ValidationResultBuilder validationBuilder = mock(ValidationResult.ValidationResultBuilder.class);

        underTest.validate(stack, validationBuilder);

        verify(parcelFilterService).filterParcelsByBlueprint(1L, 1L, Set.of(product), blueprint);
        verifyNoInteractions(validationBuilder);
    }

    @Test
    void testValidateWhenDatahubAndFilterThrowsCloudbreakRuntimeException() {
        Stack stack = createStack();

        when(stack.getComponents()).thenReturn(Collections.emptySet());

        when(parcelFilterService.filterParcelsByBlueprint(anyLong(), anyLong(), anySet(), any()))
                .thenThrow(new CloudbreakRuntimeException("filter error"));
        ValidationResult.ValidationResultBuilder validationBuilder = mock(ValidationResult.ValidationResultBuilder.class);

        underTest.validate(stack, validationBuilder);

        verify(validationBuilder).error("The validation of the configured parcels for the cluster has failed: filter error");
    }

    @Test
    void testValidateWhenDatahubAndIllegalStateExceptionThrown() {
        Stack stack = mock(Stack.class);
        when(stack.isDatahub()).thenReturn(true);

        Component component = new Component();
        component.setComponentType(ComponentType.CDH_PRODUCT_DETAILS);
        Json attributes = mock(Json.class);
        when(attributes.getUnchecked(ClouderaManagerProduct.class)).thenThrow(new IllegalStateException("json error"));
        component.setAttributes(attributes);
        when(stack.getComponents()).thenReturn(Set.of(component));

        ValidationResult.ValidationResultBuilder validationBuilder = mock(ValidationResult.ValidationResultBuilder.class);

        underTest.validate(stack, validationBuilder);

        verify(validationBuilder).error("The validation of the configured parcels for the cluster has failed: json error");
    }

    @Test
    void testValidateWhenDatahubAndSomeParcelsFilteredOut() {
        Stack stack = createStack();
        Blueprint blueprint = stack.getCluster().getBlueprint();

        ClouderaManagerProduct product1 = new ClouderaManagerProduct();
        product1.setName("product1");
        ClouderaManagerProduct product2 = new ClouderaManagerProduct();
        product2.setName("product2");

        Component component1 = createComponent(product1);
        Component component2 = createComponent(product2);

        when(stack.getComponents()).thenReturn(Set.of(component1, component2));

        when(parcelFilterService.filterParcelsByBlueprint(anyLong(), anyLong(), anySet(), any())).thenReturn(Set.of(product1));
        ValidationResult.ValidationResultBuilder validationBuilder = mock(ValidationResult.ValidationResultBuilder.class);

        underTest.validate(stack, validationBuilder);

        verify(parcelFilterService).filterParcelsByBlueprint(eq(1L), eq(1L), anySet(), eq(blueprint));
        verifyNoInteractions(validationBuilder);
    }

    private Stack createStack() {
        Stack stack = mock(Stack.class);
        when(stack.isDatahub()).thenReturn(true);
        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(1L);
        when(stack.getWorkspace()).thenReturn(workspace);
        when(stack.getId()).thenReturn(1L);
        Cluster cluster = mock(Cluster.class);
        Blueprint blueprint = mock(Blueprint.class);
        when(cluster.getBlueprint()).thenReturn(blueprint);
        when(stack.getCluster()).thenReturn(cluster);
        return stack;
    }

    private Component createComponent(ClouderaManagerProduct product) {
        Component component = new Component();
        component.setComponentType(ComponentType.CDH_PRODUCT_DETAILS);
        Json attributes = mock(Json.class);
        when(attributes.getUnchecked(ClouderaManagerProduct.class)).thenReturn(product);
        component.setAttributes(attributes);
        return component;
    }
}