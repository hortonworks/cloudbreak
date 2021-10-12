package com.sequenceiq.mock.salt.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PackageVersionResponse;
import com.sequenceiq.mock.clouderamanager.ClouderaManagerDto;
import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateHostInfo;

@Component
public class PgkVersionSaltResponse implements SaltResponse {

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        List<Map<String, String>> result = new ArrayList<>();
        PackageVersionResponse response = new PackageVersionResponse();
        ClouderaManagerDto cmDto = clouderaManagerStoreService.read(mockUuid);
        List<ApiClusterTemplateHostInfo> hosts = cmDto.getClusterTemplate().getInstantiator().getHosts();
        Map<String, String> pkgVersion = new HashMap<>();
        for (ApiClusterTemplateHostInfo host : hosts) {
            String hostName = host.getHostName();
            pkgVersion.put(hostName, cmDto.getClusterTemplate().getCmVersion());
        }
        result.add(pkgVersion);
        response.setResult(result);
        return response;
    }

    @Override
    public String cmd() {
        return "pkg.version";
    }
}
