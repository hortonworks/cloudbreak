package com.sequenceiq.freeipa.service.freeipa.user.conversion;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FmsGroupConverterTest {
    private FmsGroupConverter underTest = new FmsGroupConverter();

    @Test
    public void testUmsGroupToGroup() {
        String groupName = "foobar";
        UserManagementProto.Group umsGroup = UserManagementProto.Group.newBuilder()
                .setGroupName(groupName)
                .build();

        FmsGroup fmsGroup = underTest.umsGroupToGroup(umsGroup);

        assertEquals(groupName, fmsGroup.getName());
    }

    @Test
    public void testUmsGroupToGroupMissingName() {
        UserManagementProto.Group umsGroup = UserManagementProto.Group.newBuilder()
                .build();

        assertThrows(IllegalArgumentException.class, () -> underTest.umsGroupToGroup(umsGroup));
    }

    @Test
    public void testNameToGroup() {
        String groupName = "foobar";

        FmsGroup fmsGroup = underTest.nameToGroup(groupName);

        assertEquals(groupName, fmsGroup.getName());
    }

    @Test
    public void testNameToGroupMissingGroupName() {
        assertThrows(IllegalArgumentException.class, () -> underTest.nameToGroup(null));
    }
}