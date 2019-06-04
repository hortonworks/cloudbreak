package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.connector.PlatformAccessConfigsAction;
import com.sequenceiq.it.cloudbreak.action.v4.connector.PlatformDisksAction;
import com.sequenceiq.it.cloudbreak.action.v4.connector.PlatformEncryptionKeysAction;
import com.sequenceiq.it.cloudbreak.action.v4.connector.PlatformGatewaysAction;
import com.sequenceiq.it.cloudbreak.action.v4.connector.PlatformIpPoolsAction;
import com.sequenceiq.it.cloudbreak.action.v4.connector.PlatformNetworksAction;
import com.sequenceiq.it.cloudbreak.action.v4.connector.PlatformRegionsAction;
import com.sequenceiq.it.cloudbreak.action.v4.connector.PlatformSecurityGroupsAction;
import com.sequenceiq.it.cloudbreak.action.v4.connector.PlatformSshKeysAction;
import com.sequenceiq.it.cloudbreak.action.v4.connector.PlatformVmTypesAction;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformAccessConfigsTestDto;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformDiskTestDto;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformEncryptionKeysTestDto;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformGatewaysTestDto;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformIpPoolsTestDto;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformNetworksTestDto;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformRegionTestDto;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformSecurityGroupsTestDto;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformSshKeysTestDto;
import com.sequenceiq.it.cloudbreak.dto.connector.PlatformVmTypesTestDto;

@Service
public class ConnectorTestClient {

    public Action<PlatformAccessConfigsTestDto, CloudbreakClient> accessConfigs() {
        return new PlatformAccessConfigsAction();
    }

    public Action<PlatformDiskTestDto, CloudbreakClient> disks() {
        return new PlatformDisksAction();
    }

    public Action<PlatformEncryptionKeysTestDto, CloudbreakClient> encryptionKeys() {
        return new PlatformEncryptionKeysAction();
    }

    public Action<PlatformIpPoolsTestDto, CloudbreakClient> ipPools() {
        return new PlatformIpPoolsAction();
    }

    public Action<PlatformNetworksTestDto, CloudbreakClient> networks() {
        return new PlatformNetworksAction();
    }

    public Action<PlatformGatewaysTestDto, CloudbreakClient> gateways() {
        return new PlatformGatewaysAction();
    }

    public Action<PlatformRegionTestDto, CloudbreakClient> regions() {
        return new PlatformRegionsAction();
    }

    public Action<PlatformSecurityGroupsTestDto, CloudbreakClient> securityGroups() {
        return new PlatformSecurityGroupsAction();
    }

    public Action<PlatformSshKeysTestDto, CloudbreakClient> sshKeys() {
        return new PlatformSshKeysAction();
    }

    public Action<PlatformVmTypesTestDto, CloudbreakClient> vmTypes() {
        return new PlatformVmTypesAction();
    }

}
