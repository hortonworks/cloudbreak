package com.sequenceiq.mock.salt.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        List<String> targets = params.get("tgt");
        List<String> args = params.get("arg");
        String host = !CollectionUtils.isEmpty(targets) ? targets.get(0) : "";
        Map<String, JsonNode> result = new HashMap<>();
        String s = "(cd /srv/salt/disk;CLOUD_PLATFORM='MOCK' ATTACHED_VOLUME_NAME_LIST='' ATTACHED_VOLUME_SERIAL_LIST='' ./find-device-and-format.sh)";
        if (!CollectionUtils.isEmpty(args) && args.contains(s)) {
            result.put(host, new TextNode("a750be52-fc65-11e8-8eb2-f2801f1b9fd1"));
        } else if (!CollectionUtils.isEmpty(args) && args.contains("cat /etc/fstab")) {
            result.put(host, new TextNode("UUID=a750be52-fc65-11e8-8eb2-f2801f1b9fd1 /hadoopfs/fs1        ext4          noatime          0      2"));
        } else if (!CollectionUtils.isEmpty(args) && args.contains("salt-bootstrap version")) {
            ClouderaManagerDto cmDto = clouderaManagerStoreService.read(mockUuid);
            addDataToAllHost(cmDto, result, "3000.8");
        } else if (!CollectionUtils.isEmpty(args) && args.contains("cat /var/lib/cloudera-scm-agent/active_parcels.json | jq -r '.CDH'")) {
            ClouderaManagerDto cmDto = clouderaManagerStoreService.read(mockUuid);
            addDataToAllHost(cmDto, result, cmDto.getClusterTemplate().getCdhVersion());
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
