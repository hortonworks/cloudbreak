package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static com.sequenceiq.cloudbreak.cloud.azure.validator.Scope.MANAGEMENT_GROUP_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScopeTest {

    @Test
    public void testResourceScope() {
        Scope scope = Scope.resource("resourceId");
        assertEquals("resourceId", scope.toString());
        assertTrue(scope.match("resourceId"));
        assertTrue(scope.match("prefix/resourceId"));
        assertFalse(scope.match("resourceId/postfix"));
    }

    @Test
    public void testManagementGroupScope() {
        Scope scope = Scope.managementGroup();
        assertEquals(MANAGEMENT_GROUP_SCOPE, scope.toString());
        assertTrue(scope.match(MANAGEMENT_GROUP_SCOPE));
        assertTrue(scope.match(MANAGEMENT_GROUP_SCOPE + "/managementGroupName"));
        assertFalse(scope.match("prefix/" + MANAGEMENT_GROUP_SCOPE));
    }
}