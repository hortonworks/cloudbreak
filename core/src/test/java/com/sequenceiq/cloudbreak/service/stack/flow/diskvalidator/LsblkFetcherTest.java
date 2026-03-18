package com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.job.disk.model.InstanceResourceDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

@ExtendWith(MockitoExtension.class)
class LsblkFetcherTest {

    @Mock
    private HostOrchestrator hostOrchestrator;

    @InjectMocks
    private LsblkFetcher underTest;

    @Test
    void testGetLsblkResultsValidation() throws CloudbreakOrchestratorFailedException {
        List<GatewayConfig> gatewayConfigs = List.of();
        Set<String> targetFqdns = Set.of("host1");
        String output = """
                NAME="nvme0n1" SIZE="108000000000" MOUNTPOINT="/mnt"\n" +
                NAME="nvme1n1" SIZE="215000000000" \n" +
                NAME="nvme2n1"
                """;
        when(hostOrchestrator.runCommandOnHosts(eq(gatewayConfigs), eq(targetFqdns), anyString()))
                .thenReturn(Map.of("host1", output));

        MultiValuedMap<String, InstanceResourceDto.VolumeDto> result = underTest.getLsblkResults(gatewayConfigs, targetFqdns);

        assertThat(result.get("host1")).hasSize(3);
        List<InstanceResourceDto.VolumeDto> lines = List.copyOf(result.get("host1"));

        assertThat(lines.get(0).deviceName()).isEqualTo("nvme0n1");
        assertThat(lines.get(0).size()).isEqualTo(100);
        assertThat(lines.get(0).mountPoint()).isEqualTo("/mnt");

        assertThat(lines.get(1).deviceName()).isEqualTo("nvme1n1");
        assertThat(lines.get(1).size()).isEqualTo(200);
        assertThat(lines.get(1).mountPoint()).isNull();

        assertThat(lines.get(2).deviceName()).isEqualTo("nvme2n1");
        assertThat(lines.get(2).size()).isEqualTo(0);
        assertThat(lines.get(2).mountPoint()).isNull();
    }

    @Test
    void testGetLsblkResultsSync() throws CloudbreakOrchestratorFailedException {
        List<GatewayConfig> gatewayConfigs = List.of();
        Set<String> targetFqdns = Set.of("host1");
        String output = "NAME=\"nvme0n1\" SIZE=\"108000000000\" SERIAL=\"serial1\" UUID=\"uuid1\" MOUNTPOINT=\"/mnt\" TYPE=\"disk\" HCTL=\"hctl1\"\n" +
                "NAME=\"nvme1n1\" SIZE=\"215000000000\" UUID=\"uuid2\"\n" +
                "NAME=\"nvme2n1\" SIZE=\"324000000000\" SERIAL=\"serial3\" UUID=\"\"";

        when(hostOrchestrator.runCommandOnHosts(eq(gatewayConfigs), eq(targetFqdns), anyString()))
                .thenReturn(Map.of("host1", output));

        MultiValuedMap<String, InstanceResourceDto.VolumeDto> result = underTest.getLsblkResults(gatewayConfigs, targetFqdns);

        assertThat(result.get("host1")).hasSize(3);
        List<InstanceResourceDto.VolumeDto> lines = List.copyOf(result.get("host1"));

        assertThat(lines.get(0).deviceName()).isEqualTo("nvme0n1");
        assertThat(lines.get(0).size()).isEqualTo(100);
        assertThat(lines.get(0).mountPoint()).isEqualTo("/mnt");
        assertThat(lines.get(0).volumeType()).isEqualTo("disk");
        assertThat(lines.get(0).uuid()).isEqualTo("uuid1");
        assertThat(lines.get(0).serial()).isEqualTo("serial1");
        assertThat(lines.get(0).hctl()).isEqualTo("hctl1");

        assertThat(lines.get(1).deviceName()).isEqualTo("nvme1n1");
        assertThat(lines.get(1).size()).isEqualTo(200);
        assertThat(lines.get(1).uuid()).isEqualTo("uuid2");
    }

    @Test
    void testGetLsblkResultsSyncWithEmptyResult() throws CloudbreakOrchestratorFailedException {
        List<GatewayConfig> gatewayConfigs = List.of();
        Set<String> targetFqdns = Set.of("host1");
        when(hostOrchestrator.runCommandOnHosts(any(), any(), any())).thenReturn(Map.of("host1", ""));

        MultiValuedMap<String, InstanceResourceDto.VolumeDto> result = underTest.getLsblkResults(gatewayConfigs, targetFqdns);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void testGetLsblkResultsWhenOrchestratorFails() throws CloudbreakOrchestratorFailedException {
        List<GatewayConfig> gatewayConfigs = List.of();
        Set<String> targetFqdns = Set.of("host1");
        when(hostOrchestrator.runCommandOnHosts(any(), any(), any())).thenThrow(new CloudbreakOrchestratorFailedException("error"));

        assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.getLsblkResults(gatewayConfigs, targetFqdns));
    }
}
