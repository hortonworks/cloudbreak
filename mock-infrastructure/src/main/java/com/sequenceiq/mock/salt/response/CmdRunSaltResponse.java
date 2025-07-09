package com.sequenceiq.mock.salt.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.clouderamanager.ClouderaManagerDto;
import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateHostInfo;

@Component
public class CmdRunSaltResponse implements SaltResponse {

    public static final String ONE_HUNDRED_MB = "104857600";

    public static final String NINE_HUNDRED_MB = "943718400";

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        List<String> targets = params.get("tgt");
        List<String> args = params.get("arg");
        String host = CollectionUtils.isEmpty(targets) ? "" : targets.getFirst();
        Map<String, JsonNode> result = new HashMap<>();

        Map<String, Runnable> commandHandlers = Map.of(
                "(cd /srv/salt/disk;CLOUD_PLATFORM='MOCK' ATTACHED_VOLUME_NAME_LIST='' ATTACHED_VOLUME_SERIAL_LIST='' ./find-device-and-format.sh)",
                () -> result.put(host, new TextNode("a750be52-fc65-11e8-8eb2-f2801f1b9fd1")),
                "cat /etc/fstab",
                () -> result.put(host, new TextNode("UUID=a750be52-fc65-11e8-8eb2-f2801f1b9fd1 /hadoopfs/fs1        ext4          noatime          0      2")),
                "salt-bootstrap version",
                () -> {
                    ClouderaManagerDto cmDto = clouderaManagerStoreService.read(mockUuid);
                    addDataToAllHost(cmDto, result, "3000.8");
                },
                "cat /var/lib/cloudera-scm-agent/active_parcels.json | jq -r '.CDH'",
                () -> {
                    ClouderaManagerDto cmDto = clouderaManagerStoreService.read(mockUuid);
                    addDataToAllHost(cmDto, result, cmDto.getClusterTemplate().getCdhVersion());
                },
                "df -k /dbfs | tail -1 | awk '{print $4}'",
                () -> {
                    ClouderaManagerDto cmDto = clouderaManagerStoreService.read(mockUuid);
                    addDataToAllHost(cmDto, result, NINE_HUNDRED_MB);
                },
                "du -sk /dbfs/pgsql | awk '{print $1}'",
                () -> {
                    ClouderaManagerDto cmDto = clouderaManagerStoreService.read(mockUuid);
                    addDataToAllHost(cmDto, result, ONE_HUNDRED_MB);
                }
        );

        if (!CollectionUtils.isEmpty(args)) {
            for (String arg : args) {
                Runnable handler = commandHandlers.get(arg);
                if (handler != null) {
                    handler.run();
                    break;
                }
            }
        }

        ApplyResponse response = new ApplyResponse();
        response.setResult(List.of(result));
        return response;
    }

    private void addDataToAllHost(ClouderaManagerDto cmDto, Map<String, JsonNode> result, String data) {
        List<ApiClusterTemplateHostInfo> hosts = cmDto.getClusterTemplate().getInstantiator().getHosts();
        for (ApiClusterTemplateHostInfo host : hosts) {
            String hostName = host.getHostName();
            result.put(hostName,  new TextNode(data));
        }
    }

    @Override
    public String cmd() {
        return "cmd.run";
    }
}
