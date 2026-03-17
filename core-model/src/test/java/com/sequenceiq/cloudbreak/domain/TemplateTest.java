package com.sequenceiq.cloudbreak.domain;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.Json;

class TemplateTest {

    @Test
    void getFallbackInstanceTypesAsList() {
        Template template = new Template();
        template.setFallbackInstanceTypes(new Json("[\"instanceType1\",\"instanceType2\",\"instanceType3\"]"));
        List<String> instanceTypes = template.getFallbackInstanceTypesAsList();
        assertEquals(3, instanceTypes.size());
    }

    @Test
    void getFallbackInstanceTypesAsListNull() {
        Template template = new Template();
        List<String> instanceTypes = template.getFallbackInstanceTypesAsList();
        assertEquals(0, instanceTypes.size());
    }

    @Test
    void getFallbackInstanceTypesAsListBad() {
        Template template = new Template();
        template.setFallbackInstanceTypes(new Json("eType1\",\"instanceTy"));
        List<String> instanceTypes = template.getFallbackInstanceTypesAsList();
        assertEquals(0, instanceTypes.size());
    }
}