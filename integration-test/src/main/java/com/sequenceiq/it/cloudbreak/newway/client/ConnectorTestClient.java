package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.connector.PlatformAccessConfigsAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.connector.PlatformDisksAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.connector.PlatformEncryptionKeysAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.connector.PlatformGatewaysAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.connector.PlatformIpPoolsAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.connector.PlatformNetworksAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.connector.PlatformRegionsAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.connector.PlatformSecurityGroupsAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.connector.PlatformSshKeysAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.connector.PlatformVmTypesAction;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformAccessConfigsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformDiskTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformEncryptionKeysTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformGatewaysTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformIpPoolsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformNetworksTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformRegionTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformSecurityGroupsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformSshKeysTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.connector.PlatformVmTypesTestDto;

@Service
public class ConnectorTestClient {

    public Action<PlatformAccessConfigsTestDto> accessConfigs() {
        return new PlatformAccessConfigsAction();
    }

    public Action<PlatformDiskTestDto> disks() {
        return new PlatformDisksAction();
    }

    public Action<PlatformEncryptionKeysTestDto> encryptionKeys() {
        return new PlatformEncryptionKeysAction();
    }

    public Action<PlatformIpPoolsTestDto> ipPools() {
        return new PlatformIpPoolsAction();
    }

    public Action<PlatformNetworksTestDto> networks() {
        return new PlatformNetworksAction();
    }

    public Action<PlatformGatewaysTestDto> gateways() {
        return new PlatformGatewaysAction();
    }

    public Action<PlatformRegionTestDto> regions() {
        return new PlatformRegionsAction();
    }

    public Action<PlatformSecurityGroupsTestDto> securityGroups() {
        return new PlatformSecurityGroupsAction();
    }

    public Action<PlatformSshKeysTestDto> sshKeys() {
        return new PlatformSshKeysAction();
    }

    public Action<PlatformVmTypesTestDto> vmTypes() {
        return new PlatformVmTypesAction();
    }

}
