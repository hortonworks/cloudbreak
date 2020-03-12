package com.sequenceiq.datalake.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.service.sdx.CDPConfigKey;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Service
public class CDPConfigService {

    public static final int RUNTIME_GROUP = 1;

    public static final int CLOUDPLATFORM_GROUP = 2;

    public static final int CLUSTERSHAPE_GROUP = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPConfigService.class);

    @Value("${datalake.unsupported.runtimes:}")
    private Set<String> unsupportedRuntimes;

    private Map<CDPConfigKey, String> cdpStackRequests = new HashMap<>();

    @PostConstruct
    public void initCdpStackRequests() {
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = pathMatchingResourcePatternResolver.getResources("classpath:runtime/*/*/*.json");
            for (Resource resource : resources) {
                Matcher matcher = Pattern.compile(".*/runtime/(.*)/(.*)/(.*).json").matcher(resource.getURL().getPath());
                if (matcher.find()) {
                    String runtimeVersion = matcher.group(RUNTIME_GROUP);
                    if (!unsupportedRuntimes.contains(runtimeVersion)) {
                        CloudPlatform cloudPlatform = CloudPlatform.valueOf(matcher.group(CLOUDPLATFORM_GROUP).toUpperCase());
                        SdxClusterShape sdxClusterShape = SdxClusterShape.valueOf(matcher.group(CLUSTERSHAPE_GROUP).toUpperCase());
                        CDPConfigKey cdpConfigKey = new CDPConfigKey(cloudPlatform, sdxClusterShape, runtimeVersion);
                        String templateString = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8.name());
                        cdpStackRequests.put(cdpConfigKey, templateString);
                    }
                }
            }
            LOGGER.info("Cdp configs for datalakes: {}", cdpStackRequests);
        } catch (IOException e) {
            LOGGER.error("Can't read CDP template files", e);
            throw new IllegalStateException("Can't read CDP template files", e);
        }
    }

    public StackV4Request getConfigForKey(CDPConfigKey cdpConfigKey) {
        try {
            String cdpStackRequest = cdpStackRequests.get(cdpConfigKey);
            if (cdpStackRequest == null) {
                return null;
            } else {
                return JsonUtil.readValue(cdpStackRequest, StackV4Request.class);
            }
        } catch (IOException e) {
            LOGGER.error("Can't convert json to StackV4Request", e);
            throw new IllegalStateException("Can't convert json to StackV4Request", e);
        }
    }

    public List<String> getDatalakeVersions() {
        return cdpStackRequests.keySet().stream().map(CDPConfigKey::getRuntimeVersion).distinct().collect(Collectors.toList());
    }

}
