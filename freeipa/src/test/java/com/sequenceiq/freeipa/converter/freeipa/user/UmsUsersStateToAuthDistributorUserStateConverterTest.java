package com.sequenceiq.freeipa.converter.freeipa.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.Group;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.User;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UserState;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser.State;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

class UmsUsersStateToAuthDistributorUserStateConverterTest {

    private UmsUsersStateToAuthDistributorUserStateConverter underTest = new UmsUsersStateToAuthDistributorUserStateConverter();

    @Test
    public void testConvert() {
        UmsUsersState umsUsersState = UmsUsersState.newBuilder()
                .setUsersState(UsersState.newBuilder()
                        .addUser(new FmsUser().withName("name1").withFirstName("firstName1").withLastName("lastName1").withState(State.ENABLED))
                        .addUser(new FmsUser().withName("name2").withFirstName("firstName2").withLastName("lastName2").withState(State.DISABLED))
                        .addGroup(new FmsGroup().withName("group1"))
                        .addGroup(new FmsGroup().withName("group2"))
                        .addUserMetadata("name1", new UserMetadata("crn1", 1L))
                        .addUserMetadata("name2", new UserMetadata("crn2", 2L))
                        .addMemberToGroup("group1", "name1")
                        .addMemberToGroup("group1", "name2")
                        .build())
                .build();
        UserState result = underTest.convert(umsUsersState);

        assertThat(result.getUsersList())
                .hasSize(2)
                .extracting(User::getName, User::getFirstName, User::getLastName, User::getState)
                .containsOnly(
                        tuple("name1", "firstName1", "lastName1", User.State.ENABLED),
                        tuple("name2", "firstName2", "lastName2", User.State.DISABLED));

        assertThat(result.getGroupsList())
                .hasSize(2)
                .extracting(Group::getName)
                .containsOnly("group1", "group2");

        assertThat(result.getUserMetadataMapMap().entrySet())
                .hasSize(2)
                .extracting(Map.Entry::getKey, e -> e.getValue().getCrn(), e -> e.getValue().getWorkloadCredentialsVersion())
                .containsOnly(
                        tuple("name1", "crn1", 1L),
                        tuple("name2", "crn2", 2L));

        assertThat(result.getGroupMembershipsMap())
                .hasSize(1)
                .containsOnlyKeys("group1");
        assertThat(result.getGroupMembershipsMap().get("group1").getUserList()).containsOnly("name1", "name2");
    }

    @Test
    public void testConvertWithNullValues() {
        UmsUsersState umsUsersState = UmsUsersState.newBuilder()
                .setUsersState(UsersState.newBuilder()
                        .addUser(new FmsUser().withName(null).withFirstName(null).withLastName(null).withState(null))
                        .addGroup(new FmsGroup().withName(null))
                        .addUserMetadata("name1", new UserMetadata(null, 1L))
                        .build())
                .build();
        UserState result = underTest.convert(umsUsersState);

        assertThat(result.getUsersList())
                .hasSize(1)
                .extracting(User::getName, User::getFirstName, User::getLastName, User::getState)
                .containsOnly(
                        tuple("", "", "", User.State.ENABLED));

        assertThat(result.getGroupsList())
                .hasSize(1)
                .extracting(Group::getName)
                .containsOnly("");

        assertThat(result.getUserMetadataMapMap().entrySet())
                .hasSize(1)
                .extracting(Map.Entry::getKey, e -> e.getValue().getCrn(), e -> e.getValue().getWorkloadCredentialsVersion())
                .containsOnly(
                        tuple("name1", "", 1L));

        assertThat(result.getGroupMembershipsMap()).isEmpty();
    }
}