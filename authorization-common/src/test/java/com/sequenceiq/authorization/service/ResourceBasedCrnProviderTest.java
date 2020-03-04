package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

public class ResourceBasedCrnProviderTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testUnimplementedGetResourceCrn() {
        thrown.expect(NotImplementedException.class);
        thrown.expectMessage("Logic for getting resource CRN by resource name should have been implemented for authorization!");

        new UnimplementedServiceClass().getResourceCrnByResourceName("resourceName");
    }

    @Test
    public void testUnimplementedGetResourceCrnList() {
        thrown.expect(NotImplementedException.class);
        thrown.expectMessage("Logic for getting resource CRN list by resource name list should have been implemented for authorization!");

        new UnimplementedServiceClass().getResourceCrnListByResourceNameList(Lists.newArrayList("resourceName"));
    }

    @Test
    public void testImplementedGetResourceCrn() {
        assertEquals("done", new ImplementedServiceClass().getResourceCrnByResourceName("resourceName"));
    }

    @Test
    public void testImplementedGetResourceCrnList() {
        assertTrue(new ImplementedServiceClass().getResourceCrnListByResourceNameList(Lists.newArrayList("resourceName")).contains("done"));
    }

    private static class UnimplementedServiceClass implements ResourceBasedCrnProvider {

        @Override
        public AuthorizationResourceType getResourceType() {
            return AuthorizationResourceType.CREDENTIAL;
        }
    }

    private static class ImplementedServiceClass implements ResourceBasedCrnProvider {

        @Override
        public String getResourceCrnByResourceName(String resourceName) {
            return "done";
        }

        @Override
        public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
            return Lists.newArrayList("done");
        }

        @Override
        public AuthorizationResourceType getResourceType() {
            return AuthorizationResourceType.CREDENTIAL;
        }
    }
}
