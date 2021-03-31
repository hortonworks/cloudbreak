package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

@Component
public class SshJClientActions extends SshJClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClientActions.class);

    private List<String> getSdxInstanceGroupIps(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames, boolean publicIp) {
        List<String> instanceIPs = new ArrayList<>();

        hostGroupNames.forEach(hostGroupName -> {
            InstanceMetaDataV4Response instanceMetaDataV4Response = Objects.requireNonNull(instanceGroups.stream()
                    .filter(instanceGroup -> instanceGroup.getName().equals(hostGroupName))
                    .findFirst().orElse(null)).getMetadata().stream().findFirst().orElse(null);
            assert instanceMetaDataV4Response != null;
            LOGGER.info("The selected Instance Group [{}] and the available Private IP [{}] and Public IP [{}]]. {} ip will be used.",
                    instanceMetaDataV4Response.getInstanceGroup(), instanceMetaDataV4Response.getPrivateIp(), instanceMetaDataV4Response.getPublicIp(),
                    publicIp ? "Public" : "Private");
            instanceIPs.add(publicIp ? instanceMetaDataV4Response.getPublicIp() : instanceMetaDataV4Response.getPrivateIp());
        });

        return instanceIPs;
    }

    private List<String> getFreeIpaInstanceGroupIps(String environmentCrn, FreeIpaClient freeipaClient, boolean publicIp) {
        List<String> instanceIPs = new ArrayList<>();

        freeipaClient.getDefaultClient().getFreeIpaV1Endpoint()
                .describe(environmentCrn).getInstanceGroups().stream()
                .forEach(ig -> {
                    InstanceMetaDataResponse instanceMetaDataResponse = ig.getMetaData().stream().findFirst().orElse(null);
                    assert instanceMetaDataResponse != null;
                    LOGGER.info("The selected Instance Group [{}] and the available Private IP [{}] and Public IP [{}]]. {} ip will be used.",
                            instanceMetaDataResponse.getInstanceGroup(), instanceMetaDataResponse.getPrivateIp(), instanceMetaDataResponse.getPublicIp(),
                            publicIp ? "Public" : "Private");
                    instanceIPs.add(publicIp ? instanceMetaDataResponse.getPublicIp() : instanceMetaDataResponse.getPrivateIp());
                });

        return instanceIPs;
    }

    public SdxInternalTestDto checkFilesByNameAndPath(SdxInternalTestDto testDto, List<InstanceGroupV4Response> instanceGroups,
            List<String> hostGroupNames, String filePath, String fileName, long requiredNumberOfFiles, String user, String password) {
        String fileListCommand = String.format("find %s -type f -name %s", filePath, fileName);
        AtomicLong quantity = new AtomicLong(0);

        /**
         * Right now only the Private IP is available for an Instance.
         */
        getSdxInstanceGroupIps(instanceGroups, hostGroupNames, false).forEach(instanceIP -> {
            LOGGER.info("Creating SSH client on '{}' host with user: '{}' and password: '{}'.", instanceIP, user, password);
            try (SSHClient client = createSshClient(instanceIP, user, password, null)) {
                quantity.set(executefileListCommand(instanceIP, fileListCommand, client));
            } catch (Exception e) {
                LOGGER.error("Create SSH client is failing on '{}' host with user: '{}' and password: '{}'!", instanceIP, user, password);
                throw new TestFailException(String.format(" Create SSH client is failing on '%s' host with user: '%s' and password: '%s'! ",
                        instanceIP, user, password), e);
            }
        });

        if (requiredNumberOfFiles == quantity.get()) {
            Log.log(LOGGER, format(" File '%s' is available at [%s] host group(s). ", filePath, hostGroupNames.toString()));
        } else {
            LOGGER.error("File '{}' is NOT available at [{}] host group(s)!", filePath, hostGroupNames.toString());
            throw new TestFailException(String.format("File '%s' is NOT available at [%s] host group(s)!", filePath, hostGroupNames.toString()));
        }
        return testDto;
    }

    public Map<String, Pair<Integer, String>> executeSshCommand(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames, String sshCommand,
            boolean publicIp) {
        return getSdxInstanceGroupIps(instanceGroups, hostGroupNames, publicIp).stream()
                .collect(Collectors.toMap(ip -> ip, ip -> executeSshCommand(ip, sshCommand)));
    }

    public SdxTestDto checkNoOutboundInternetTraffic(SdxTestDto testDto, List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames) {
        getSdxInstanceGroupIps(instanceGroups, hostGroupNames, true).forEach(this::checkNoOutboundInternetTraffic);
        return testDto;
    }

    public FreeIpaTestDto checkNoOutboundInternetTraffic(FreeIpaTestDto testDto, FreeIpaClient freeIpaClient) {
        getFreeIpaInstanceGroupIps(testDto.getResponse().getEnvironmentCrn(), freeIpaClient, true)
                .forEach(this::checkNoOutboundInternetTraffic);
        return testDto;
    }

    private void checkNoOutboundInternetTraffic(String instanceIp) {
        Pair<Integer, String> cmdOut = executeSshCommand(instanceIp, "curl --max-time 30 cloudera.com");
        if (cmdOut.getKey() == 0) {
            throw new TestFailException("Instance [" + instanceIp + "] has internet coonection but shouldn't have!");
        }
    }

    private Pair<Integer, String> executeSshCommand(String instanceIp, String command) {
        try (SSHClient sshClient = createSshClient(instanceIp, null, null, null)) {
            Pair<Integer, String> cmdOut = execute(sshClient, command);
            Log.log(LOGGER, format("Command exit status [%s] and result [%s].", cmdOut.getKey(), cmdOut.getValue()));
            return cmdOut;
        } catch (Exception e) {
            LOGGER.error("SSH fail on [{}] while executing command [{}]", instanceIp, command);
            throw new TestFailException(" SSH fail on [" + instanceIp + "] while executing command [" + command + "].", e);
        }
    }

    private long executefileListCommand(String instanceIP, String fileListCommand, SSHClient sshClient) {
        AtomicLong quantity = new AtomicLong(0);

        try {
            Pair<Integer, String> cmdOut = execute(sshClient, fileListCommand);
            Log.log(LOGGER, format(" Command exit status '%s' and result '%s'. ", cmdOut.getKey(), cmdOut.getValue()));

            List<String> cmdOutputValues = List.of(cmdOut.getValue().split("[\\r\\n\\t]"))
                    .stream().filter(Objects::nonNull).collect(Collectors.toList());
            boolean fileFound = cmdOutputValues.stream()
                    .anyMatch(outputValue -> outputValue.strip().startsWith("/"));
            String foundFilePath = cmdOutputValues.stream()
                    .filter(outputValue -> outputValue.strip().startsWith("/")).findFirst().orElse(null);
            Log.log(LOGGER, format(" The file is present '%s' at '%s' path. ", fileFound, foundFilePath));

            quantity.set(cmdOutputValues.stream()
                    .filter(outputValue -> outputValue.strip().startsWith("/")).count());
        } catch (Exception e) {
            LOGGER.error("SSH fail on '{}' host while running command: [{}]", instanceIP, fileListCommand);
            throw new TestFailException(String.format(" SSH fail on '%s' host while running command: [%s]! ", instanceIP, fileListCommand), e);
        }
        return quantity.get();
    }
}
