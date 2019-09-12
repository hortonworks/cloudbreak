package com.sequenceiq.freeipa.kerberosmgmt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.RoleRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Privilege;
import com.sequenceiq.freeipa.client.model.Role;
import com.sequenceiq.freeipa.client.model.Service;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtRoleComponent;

@ExtendWith(MockitoExtension.class)
public class KerberosMgmtRoleComponentV1Test {
    private static final String HOST = "host1";

    private static final String ROLE = "role1";

    private static final String SERVICE = "service1";

    private static final String PRIVILEGE1 = "privilege1";

    private static final String PRIVILEGE2 = "privilege2";

    private static final String ERROR_MESSAGE = "error message";

    private static final int NOT_FOUND = 4001;

    @Mock
    private FreeIpaClient mockIpaClient;

    @Test
    public void testAddRoleAndPrivilegesForHostWithoutRole() throws Exception {
        Host host = new Host();
        host.setFqdn(HOST);
        RoleRequest roleRequest = null;
        new KerberosMgmtRoleComponent().addRoleAndPrivileges(Optional.empty(), Optional.of(host), roleRequest, mockIpaClient);
        Mockito.verifyZeroInteractions(mockIpaClient);
    }

    @Test
    public void testAddRoleAndPrivilegesForHostWithRole() throws Exception {
        Host host = new Host();
        host.setFqdn(HOST);
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setRoleName(ROLE);
        Set<String> privileges = new HashSet<>();
        privileges.add(PRIVILEGE1);
        privileges.add(PRIVILEGE2);
        roleRequest.setPrivileges(privileges);
        Set<Role> noRoles = new HashSet<Role>();
        Role role = new Role();
        role.setCn(ROLE);
        Mockito.when(mockIpaClient.addRole(anyString())).thenReturn(role);
        Privilege privilege = new Privilege();
        Set<String> hosts = new HashSet<>();
        hosts.add(HOST);
        Set<String> noServices = new HashSet<>();
        Mockito.when(mockIpaClient.findAllRole()).thenReturn(noRoles);
        Mockito.when(mockIpaClient.showPrivilege(any())).thenReturn(privilege);
        Mockito.when(mockIpaClient.addRolePrivileges(any(), any())).thenReturn(role);
        Mockito.when(mockIpaClient.showRole(anyString())).thenReturn(role);
        Mockito.when(mockIpaClient.addRoleMember(any(), any(), any(), any(), any(), any())).thenReturn(role);
        new KerberosMgmtRoleComponent().addRoleAndPrivileges(Optional.empty(), Optional.of(host), roleRequest, mockIpaClient);
        Mockito.verify(mockIpaClient).addRole(ROLE);
        Mockito.verify(mockIpaClient).addRolePrivileges(ROLE, privileges);
        Mockito.verify(mockIpaClient).addRoleMember(ROLE, null, null, hosts, null, noServices);
    }

    @Test
    public void testAddRoleAndPrivilegesForServiceWithoutRole() throws Exception {
        Service service = new Service();
        service.setKrbprincipalname(SERVICE);
        RoleRequest roleRequest = null;
        new KerberosMgmtRoleComponent().addRoleAndPrivileges(Optional.of(service), Optional.empty(), roleRequest, mockIpaClient);
        Mockito.verifyZeroInteractions(mockIpaClient);
    }

    @Test
    public void testAddRoleAndPrivilegesForServiceWithRole() throws Exception {
        Service service = new Service();
        service.setKrbprincipalname(SERVICE);
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setRoleName(ROLE);
        Set<String> privileges = new HashSet<>();
        privileges.add(PRIVILEGE1);
        privileges.add(PRIVILEGE2);
        roleRequest.setPrivileges(privileges);
        Set<Role> noRoles = new HashSet<Role>();
        Role role = new Role();
        role.setCn(ROLE);
        Mockito.when(mockIpaClient.addRole(anyString())).thenReturn(role);
        Privilege privilege = new Privilege();
        Set<String> noHosts = new HashSet<>();
        Set<String> services = new HashSet<>();
        services.add(SERVICE);
        Mockito.when(mockIpaClient.findAllRole()).thenReturn(noRoles);
        Mockito.when(mockIpaClient.showPrivilege(any())).thenReturn(privilege);
        Mockito.when(mockIpaClient.addRolePrivileges(any(), any())).thenReturn(role);
        Mockito.when(mockIpaClient.showRole(anyString())).thenReturn(role);
        Mockito.when(mockIpaClient.addRoleMember(any(), any(), any(), any(), any(), any())).thenReturn(role);
        new KerberosMgmtRoleComponent().addRoleAndPrivileges(Optional.of(service), Optional.empty(), roleRequest, mockIpaClient);
        Mockito.verify(mockIpaClient).addRole(ROLE);
        Mockito.verify(mockIpaClient).addRolePrivileges(ROLE, privileges);
        Mockito.verify(mockIpaClient).addRoleMember(ROLE, null, null, noHosts, null, services);
    }

    @Test
    public void testDeleteRoleIfNoLongerUsedWhenRoleIsNull() throws Exception {
        new KerberosMgmtRoleComponent().deleteRoleIfItIsNoLongerUsed(null, mockIpaClient);
        Mockito.verifyZeroInteractions(mockIpaClient);
    }

    @Test
    public void testDeleteRoleIfNoLongerUsedWhenRoleDoesNotExist() throws Exception {
        Mockito.when(mockIpaClient.showRole(anyString())).thenThrow(new FreeIpaClientException(ERROR_MESSAGE,
                new JsonRpcClientException(NOT_FOUND, ERROR_MESSAGE, null)));
        new KerberosMgmtRoleComponent().deleteRoleIfItIsNoLongerUsed(ROLE, mockIpaClient);
        Mockito.verify(mockIpaClient, Mockito.never()).deleteRole(ROLE);
    }

    @Test
    public void testDeleteRoleIfNoLongerUsedWhenRoleIsStillUsedAsMemberHost() throws Exception {
        Role role = new Role();
        role.setCn(ROLE);
        List<String> hosts = new ArrayList<>();
        hosts.add(HOST);
        role.setMemberHost(hosts);
        Mockito.when(mockIpaClient.showRole(anyString())).thenReturn(role);
        new KerberosMgmtRoleComponent().deleteRoleIfItIsNoLongerUsed(ROLE, mockIpaClient);
        Mockito.verify(mockIpaClient, Mockito.never()).deleteRole(ROLE);
    }

    @Test
    public void testDeleteRoleIfNoLongerUsedWhenRoleIsStillUsedAsMemberService() throws Exception {
        Role role = new Role();
        role.setCn(ROLE);
        List<String> services = new ArrayList<>();
        services.add(SERVICE);
        role.setMemberService(services);
        Mockito.when(mockIpaClient.showRole(anyString())).thenReturn(role);
        new KerberosMgmtRoleComponent().deleteRoleIfItIsNoLongerUsed(ROLE, mockIpaClient);
        Mockito.verify(mockIpaClient, Mockito.never()).deleteRole(ROLE);
    }

    @Test
    public void testDeleteRoleIfNoLongerUsedWhenRoleIsNotUsed() throws Exception {
        Role role = new Role();
        role.setCn(ROLE);
        Mockito.when(mockIpaClient.showRole(anyString())).thenReturn(role);
        new KerberosMgmtRoleComponent().deleteRoleIfItIsNoLongerUsed(ROLE, mockIpaClient);
        Mockito.verify(mockIpaClient).deleteRole(ROLE);
    }
}