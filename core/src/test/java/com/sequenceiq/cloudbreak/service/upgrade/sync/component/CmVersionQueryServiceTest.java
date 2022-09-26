package com.sequenceiq.cloudbreak.service.upgrade.sync.component;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.model.PackageInfo;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.Package;
import com.sequenceiq.cloudbreak.service.cluster.PackageName;

@ExtendWith(MockitoExtension.class)
class CmVersionQueryServiceTest {

    private static final String CLOUDERA_MANAGER_AGENT = "cloudera-manager-agent";

    private static final String CLOUDERA_MANAGER_SERVER = "cloudera-manager-server";

    private static final String VALID_PATTERN = "(.*)-([0-9]+)[a-zA-z]*\\..*";

    private static final String HOST_1 = "host1";

    private static final String HOST_2 = "host2";

    private static final String A_VERSION = "1";

    private static final String OTHER_VERSION = "2";

    private static final String A_BUILD_NUMBER = "1000";

    private static final String OTHER_BUILD_NUMBER = "2000";

    private static final String LOWER_BUILD_NUMBER = "26761258";

    private static final String HIGHER_BUILD_NUMBER = "32005272";

    private static final String C_VERSION = "7.4.1";

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private GatewayConfig gatewayConfig;

    @InjectMocks
    private CmVersionQueryService underTest;

    @BeforeEach
    void setup() {
        underTest.setPackages(Lists.newArrayList(getCmPackage(), getOtherPackage()));
    }

    @Test
    void testQueryCmPackageInfo() throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any(Stack.class))).thenReturn(gatewayConfig);
        Stack stack = createStack();

        Map<String, List<PackageInfo>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put(HOST_1, getPackageInfoList(true));
        hostPackageMap.put(HOST_2, getPackageInfoList(true));
        when(hostOrchestrator.getFullPackageVersionsFromAllHosts(any(GatewayConfig.class), any())).thenReturn(hostPackageMap);

        Map<String, List<PackageInfo>> packageInfo = underTest.queryCmPackageInfo(stack);

        Assertions.assertEquals(2, packageInfo.get(HOST_1).size());
        Assertions.assertEquals(2, packageInfo.get(HOST_2).size());

        Map<String, Optional<String>> packages = Maps.newHashMap();
        packages.put(CLOUDERA_MANAGER_AGENT, Optional.of(VALID_PATTERN));
        packages.put(CLOUDERA_MANAGER_SERVER, Optional.of(VALID_PATTERN));
        verify(hostOrchestrator).getFullPackageVersionsFromAllHosts(gatewayConfig, packages);

    }

    @Test
    void testWhenPackagesAreValidThenValidateConsistencyShouldPass() {
        Map<String, List<PackageInfo>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put(HOST_1, getPackageInfoList(true));
        hostPackageMap.put(HOST_2, getPackageInfoList(true));

        PackageInfo packageInfo = underTest.checkCmPackageInfoConsistency(hostPackageMap);

        Assertions.assertEquals("1", packageInfo.getVersion());
        Assertions.assertEquals("1000", packageInfo.getBuildNumber());
        Assertions.assertEquals("1-1000", packageInfo.getFullVersionPrettyPrinted());
    }

    @Test
    void testWhenAgentHasMultiplePackageVersionsThenValidateConsistencyShouldFail() {
        Map<String, List<PackageInfo>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put(HOST_1, getPackageInfoList(true));
        hostPackageMap.put(HOST_2, getPackageInfoList(false));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.checkCmPackageInfoConsistency(hostPackageMap));

        Assertions.assertEquals("Error during sync! The following package(s) has multiple versions present on the machines. "
                + "Package: [PackageInfo{name='cloudera-manager-agent', version='2', buildNumber='2000'}, "
                + "PackageInfo{name='cloudera-manager-agent', version='1', buildNumber='1000'}]", exception.getMessage());
    }

    @Test
    void testWhenServerHasMultiplePackageVersionsThenValidateConsistencyShouldPassAndChooseLatestVersion() {
        Map<String, List<PackageInfo>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put(HOST_1, List.of(
                getPackageInfo(CLOUDERA_MANAGER_SERVER, A_VERSION, A_BUILD_NUMBER),
                getPackageInfo(CLOUDERA_MANAGER_AGENT, A_VERSION, A_BUILD_NUMBER)));
        hostPackageMap.put(HOST_2, List.of(
                getPackageInfo(CLOUDERA_MANAGER_SERVER, OTHER_VERSION, OTHER_BUILD_NUMBER),
                getPackageInfo(CLOUDERA_MANAGER_AGENT, A_VERSION, A_BUILD_NUMBER)));

        PackageInfo packageInfo = underTest.checkCmPackageInfoConsistency(hostPackageMap);

        Assertions.assertEquals(packageInfo.getVersion(), OTHER_VERSION);
        Assertions.assertEquals(packageInfo.getBuildNumber(), OTHER_BUILD_NUMBER);
    }

    @Test
    void testWhenServerHasMultiplePackageVersionsDifferingInBuildOnlyThenValidateConsistencyShouldPassAndChooseLatestVersion() {
        Map<String, List<PackageInfo>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put(HOST_2, List.of(
                getPackageInfo(CLOUDERA_MANAGER_SERVER, C_VERSION, LOWER_BUILD_NUMBER),
                getPackageInfo(CLOUDERA_MANAGER_AGENT, C_VERSION, HIGHER_BUILD_NUMBER)));
        hostPackageMap.put(HOST_1, List.of(
                getPackageInfo(CLOUDERA_MANAGER_SERVER, C_VERSION, HIGHER_BUILD_NUMBER),
                getPackageInfo(CLOUDERA_MANAGER_AGENT, C_VERSION, HIGHER_BUILD_NUMBER)));

        PackageInfo packageInfo = underTest.checkCmPackageInfoConsistency(hostPackageMap);

        Assertions.assertEquals(C_VERSION, packageInfo.getVersion());
        Assertions.assertEquals(HIGHER_BUILD_NUMBER, packageInfo.getBuildNumber());
    }

    @Test
    void testWhenNoPackageVersionsThenValidateConsistencyShouldFail() {
        Map<String, List<PackageInfo>> hostPackageMap = Maps.newHashMap();
        hostPackageMap.put(HOST_1, new ArrayList<>());
        hostPackageMap.put(HOST_2, new ArrayList<>());

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.checkCmPackageInfoConsistency(hostPackageMap));

        Assertions.assertEquals("Error during sync! CM server and agent versions cannot be determined!", exception.getMessage());

    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setCluster(new Cluster());
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType("salt");
        stack.setOrchestrator(orchestrator);
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        stack.setInstanceGroups(instanceGroups);
        return stack;
    }

    private PackageName generatePackageName(String pkg, String pattern, boolean validate) {
        PackageName packageName = new PackageName();
        packageName.setName(pkg);
        packageName.setPattern(pattern);
        packageName.setValidateForMultipleVersions(validate);
        return packageName;
    }

    private Package getCmPackage() {
        Package cmPackage = new Package();
        cmPackage.setName("cm");
        cmPackage.setPkg(Lists.newArrayList(
                generatePackageName(CLOUDERA_MANAGER_AGENT, VALID_PATTERN, true),
                generatePackageName(CLOUDERA_MANAGER_SERVER, VALID_PATTERN, false)));
        return cmPackage;
    }

    private Package getOtherPackage() {
        Package otherPackage = new Package();
        otherPackage.setName("other");
        otherPackage.setPkg(Lists.newArrayList(
                generatePackageName("other-package", VALID_PATTERN, true)));
        return otherPackage;
    }

    private List<PackageInfo> getPackageInfoList(boolean matchVersions) {
        List<PackageInfo> packageMap = new ArrayList<>();
        packageMap.add(getPackageInfo(CLOUDERA_MANAGER_SERVER, A_VERSION, A_BUILD_NUMBER));
        PackageInfo server = null;
        if (matchVersions) {
            server = getPackageInfo(CLOUDERA_MANAGER_AGENT, A_VERSION, A_BUILD_NUMBER);
        } else {
            server = getPackageInfo(CLOUDERA_MANAGER_AGENT, OTHER_VERSION, OTHER_BUILD_NUMBER);
        }
        packageMap.add(server);
        return packageMap;
    }

    private PackageInfo getPackageInfo(String name, String version, String buildNumber) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setName(name);
        packageInfo.setVersion(version);
        packageInfo.setBuildNumber(buildNumber);
        return packageInfo;
    }
}