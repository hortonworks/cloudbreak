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
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.client.model.Privilege;
import com.sequenceiq.freeipa.client.model.Role;
import com.sequenceiq.freeipa.client.model.Service;

@Component
public class KerberosMgmtRoleComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosMgmtRoleComponent.class);

    public boolean privilegesExist(RoleRequest roleRequest, FreeIpaClient ipaClient) {
        return roleRequest == null ||
            roleRequest.getPrivileges().stream().allMatch(privilegeName -> {
                try {
                    ipaClient.showPrivilege(privilegeName);
                    return true;
                } catch (FreeIpaClientException e) {
                    if (!FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                        LOGGER.debug("Privilege [{}] does not exist", privilegeName);
                    } else {
                        LOGGER.error("Privilege [{}] show error", privilegeName, e);
                    }
                    return false;
                }
            });
    }

    public void addRoleAndPrivileges(Optional<Service> service, Optional<Host> host, RoleRequest roleRequest,
            FreeIpaClient ipaClient)
            throws FreeIpaClientException {
        if (roleRequest != null && StringUtils.isNotBlank(roleRequest.getRoleName())) {
            Set<Role> allRole = ipaClient.findAllRole();
            Optional<Role> optionalRole = allRole.stream().filter(role -> role.getCn().equals(roleRequest.getRoleName())).findFirst();
            Role role = optionalRole.isPresent() ? optionalRole.get() : ipaClient.addRole(roleRequest.getRoleName());
            addPrivilegesToRole(roleRequest.getPrivileges(), ipaClient, role);
            role = ipaClient.showRole(role.getCn());
            Set<String> servicesToAssignRole = service.stream()
                    .filter(s -> s.getMemberOfRole().stream().noneMatch(member -> member.contains(roleRequest.getRoleName())))
                    .map(Service::getKrbcanonicalname)
                    .collect(Collectors.toSet());
            Set<String> hostsToAssignRole = host.stream()
                    .filter(h -> h.getMemberOfRole().stream().noneMatch(member -> member.contains(roleRequest.getRoleName())))
                    .map(Host::getFqdn)
                    .collect(Collectors.toSet());
            ipaClient.addRoleMember(role.getCn(), null, null, hostsToAssignRole, null, servicesToAssignRole);
        }
    }

    private void addPrivilegesToRole(Set<String> privileges, FreeIpaClient ipaClient, Role role) throws FreeIpaClientException {
        if (privileges != null) {
            Set<String> privilegesToAdd = privileges.stream().filter(privilegeName -> {
                try {
                    Privilege privilege = ipaClient.showPrivilege(privilegeName);
                    return privilege.getMember().stream().noneMatch(member -> member.equals(role.getCn()));
                } catch (FreeIpaClientException e) {
                    LOGGER.error("Privilege [{}] show error", privilegeName, e);
                    return false;
                }
            }).collect(Collectors.toSet());
            if (!privilegesToAdd.isEmpty()) {
                ipaClient.addRolePrivileges(role.getCn(), privilegesToAdd);
            }
        }
    }

    public void deleteRoleIfItIsNoLongerUsed(String role, FreeIpaClient ipaClient) throws FreeIpaClientException {
        if (role == null) {
            return;
        }

        try {
            Role ipaRole = ipaClient.showRole(role);
            List<String> usesOfRole = new ArrayList<>();
            usesOfRole.addAll(ipaRole.getMemberUser());
            usesOfRole.addAll(ipaRole.getMemberGroup());
            usesOfRole.addAll(ipaRole.getMemberHost());
            usesOfRole.addAll(ipaRole.getMemberHostGroup());
            usesOfRole.addAll(ipaRole.getMemberService());
            if (usesOfRole.isEmpty()) {
                ipaClient.deleteRole(role);
            } else {
                LOGGER.debug("The role {} is still in use, so it was not deleted.", role);
            }
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                LOGGER.debug("The role {} does not exist, so it was not deleted.", role);
            } else {
                throw e;
            }
        }
    }
}
