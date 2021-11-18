package com.sequenceiq.freeipa.kerberosmgmt.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.RoleRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionWrapper;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Privilege;
import com.sequenceiq.freeipa.client.model.Role;
import com.sequenceiq.freeipa.client.model.Service;

@Component
public class KerberosMgmtRoleComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosMgmtRoleComponent.class);

    public boolean privilegesExist(RoleRequest roleRequest, FreeIpaClient ipaClient) throws FreeIpaClientException {
        try {
            return roleRequest == null || doAllPrivilegeExist(roleRequest.getPrivileges(), ipaClient);
        } catch (FreeIpaClientExceptionWrapper e) {
            throw e.getWrappedException();
        }
    }

    private boolean doAllPrivilegeExist(Set<String> privileges, FreeIpaClient ipaClient) {
        return privileges.stream().allMatch(privilegeName -> doesPrivilegeExist(ipaClient, privilegeName));
    }

    private boolean doesPrivilegeExist(FreeIpaClient ipaClient, String privilegeName) {
        try {
            ipaClient.showPrivilege(privilegeName);
            LOGGER.debug("Privilege [{}] exists", privilegeName);
            return true;
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                LOGGER.debug("Privilege [{}] does not exist", privilegeName);
                return false;
            } else {
                LOGGER.error("Privilege [{}] show error", privilegeName, e);
                throw new FreeIpaClientExceptionWrapper(e);
            }
        }
    }

    public void addRoleAndPrivileges(Optional<Service> service, Optional<Host> host, RoleRequest roleRequest, FreeIpaClient ipaClient)
            throws FreeIpaClientException {
        if (roleRequest != null && StringUtils.isNotBlank(roleRequest.getRoleName())) {
            Role role = fetchOrCreateRole(roleRequest, ipaClient);
            addPrivilegesToRole(roleRequest.getPrivileges(), ipaClient, role);
            Set<String> servicesToAssignRole = service.stream()
                    .filter(s -> s.getMemberOfRole().stream().noneMatch(member -> member.contains(roleRequest.getRoleName())))
                    .map(Service::getKrbcanonicalname)
                    .collect(Collectors.toSet());
            Set<String> hostsToAssignRole = host.stream()
                    .filter(h -> h.getMemberOfRole().stream().noneMatch(member -> member.contains(roleRequest.getRoleName())))
                    .map(Host::getFqdn)
                    .collect(Collectors.toSet());
            LOGGER.debug("Adding role [{}] to host {} and service {}", role.getCn(), hostsToAssignRole, servicesToAssignRole);
            ipaClient.addRoleMember(role.getCn(), null, null, hostsToAssignRole, null, servicesToAssignRole);
        } else {
            LOGGER.debug("RoleRequest or role name is empty, skipping adding privileges. {}", roleRequest);
        }
    }

    private Role fetchOrCreateRole(RoleRequest roleRequest, FreeIpaClient ipaClient) throws FreeIpaClientException {
        try {
            Optional<Role> optionalRole = FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue(() -> ipaClient.showRole(roleRequest.getRoleName()),
                    "Role [{}} not found", roleRequest.getRoleName());
            LOGGER.debug("Fetched role: {}", optionalRole);
            return optionalRole.isPresent() ? optionalRole.get() : ipaClient.addRole(roleRequest.getRoleName());
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isDuplicateEntryException(e)) {
                LOGGER.debug("The role [{}] was recently created in a different thread, retrieve it", roleRequest.getRoleName(), e);
                return ipaClient.showRole(roleRequest.getRoleName());
            } else {
                LOGGER.error("Failed to add the role [{}]", roleRequest.getRoleName(), e);
                throw e;
            }
        }
    }

    private void addPrivilegesToRole(Set<String> privileges, FreeIpaClient ipaClient, Role role) throws FreeIpaClientException {
        try {
            if (privileges != null) {
                addMissingPrivileges(privileges, ipaClient, role);
            } else {
                LOGGER.debug("Privileges is null for [{}] role", role.getCn());
            }
        } catch (FreeIpaClientExceptionWrapper e) {
            throw e.getWrappedException();
        }
    }

    private void addMissingPrivileges(Set<String> privileges, FreeIpaClient ipaClient, Role role) throws FreeIpaClientException {
        Set<String> privilegesToAdd = privileges.stream()
                .filter(privilegeName -> isPrivilegeMissingForRole(ipaClient, role, privilegeName))
                .collect(Collectors.toSet());
        if (!privilegesToAdd.isEmpty()) {
            LOGGER.debug("Privileges missing {} from {} for [{}] role", privilegesToAdd, privileges, role.getCn());
            ipaClient.addRolePrivileges(role.getCn(), privilegesToAdd);
        } else {
            LOGGER.debug("All of {} are set for [{}] role", privileges, role.getCn());
        }
    }

    private boolean isPrivilegeMissingForRole(FreeIpaClient ipaClient, Role role, String privilegeName) {
        try {
            Privilege privilege = ipaClient.showPrivilege(privilegeName);
            return privilege.getMember().stream().noneMatch(member -> member.equals(role.getCn()));
        } catch (FreeIpaClientException e) {
            LOGGER.error("Privilege [{}] show error", privilegeName, e);
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    public void deleteRoleIfItIsNoLongerUsed(String role, FreeIpaClient ipaClient) throws FreeIpaClientException {
        if (role != null) {
            Optional<Role> optionalRole = FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue(() -> ipaClient.showRole(role),
                    "Role [{}} not found", role);
            if (optionalRole.isPresent()) {
                Role ipaRole = optionalRole.get();
                List<String> usesOfRole = new ArrayList<>();
                usesOfRole.addAll(ipaRole.getMemberUser());
                usesOfRole.addAll(ipaRole.getMemberGroup());
                usesOfRole.addAll(ipaRole.getMemberHost());
                usesOfRole.addAll(ipaRole.getMemberHostGroup());
                usesOfRole.addAll(ipaRole.getMemberService());
                if (usesOfRole.isEmpty()) {
                    FreeIpaClientExceptionUtil.ignoreNotFoundException(() -> ipaClient.deleteRole(role),
                            "The role [{}] does not exist, so it was not deleted.", role);
                } else {
                    LOGGER.debug("The role {} is still in use, so it was not deleted.", role);
                }
            }
        }
    }
}
