package com.sequenceiq.cloudbreak.cmtemplate.metering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@ExtendWith(MockitoExtension.class)
public class MeteringServiceFieldResolverTest {

    private static final String SERVICE_TYPE_MAPPING_LOCATION = "metering/cm-metering-service-field-mapping.json";

    private static MeteringServiceFieldResolver underTest;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @BeforeAll
    public static void setUpGlobal() {
        underTest = new MeteringServiceFieldResolver(SERVICE_TYPE_MAPPING_LOCATION);
        underTest.init();
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testResolveServiceTypeDefault() {
        // GIVEN
        // WHEN
        String result = underTest.resolveServiceType(cmTemplateProcessor);
        // THEN
        assertEquals("DATAHUB", result);
    }

    @Test
    public void testResolveServiceTypeForOpDB() {
        // GIVEN
        given(cmTemplateProcessor.getAllComponents()).willReturn(createServiceComponents("HBASE"));
        // WHEN
        String result = underTest.resolveServiceType(cmTemplateProcessor);
        // THEN
        assertEquals("OpDB", result);
    }

    @Test
    public void testResolveServiceTypeForMultipleNifiMatch() {
        // GIVEN
        given(cmTemplateProcessor.getAllComponents()).willReturn(createServiceComponents("NIFI"));
        // WHEN
        String result = underTest.resolveServiceType(cmTemplateProcessor);
        // THEN
        assertEquals("NIFI", result);
    }

    @Test
    public void testResolveServiceTypeForMultipleMatch() {
        // GIVEN
        given(cmTemplateProcessor.getAllComponents()).willReturn(createServiceComponents("NIFI", "HBASE"));
        // WHEN
        String result = underTest.resolveServiceType(cmTemplateProcessor);
        // THEN
        assertEquals("NIFI", result);
    }

    @Test
    public void testResolveServiceFeatureForNifi() {
        // GIVEN
        given(cmTemplateProcessor.getAllComponents()).willReturn(createServiceComponents("NIFI"));
        // WHEN
        String result = underTest.resolveServiceFeature(cmTemplateProcessor);
        // THEN
        assertEquals("NIFI", result);
    }

    @Test
    public void testResolveServiceFeatureWithoutMatchingService() {
        // GIVEN
        given(cmTemplateProcessor.getAllComponents()).willReturn(createServiceComponents("HBASE"));
        // WHEN
        String result = underTest.resolveServiceFeature(cmTemplateProcessor);
        // THEN
        assertNull(result);
    }

    private Set<ServiceComponent> createServiceComponents(String... services) {
        Set<ServiceComponent> serviceComponents = new HashSet<>();
        for (String service : services) {
            serviceComponents.add(ServiceComponent.of(service, "DUMMY"));
        }
        return serviceComponents;
    }
}
