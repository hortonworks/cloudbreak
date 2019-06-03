package com.sequenceiq.cloudbreak.cm.util;

import java.util.Map;

import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;

public class ConfigUtils {

    private ConfigUtils() {
    }

    public static ApiConfigList makeApiConfigList(Map<String, String> keyValues) {
        final ApiConfigList configList = new ApiConfigList();
        for (Map.Entry<String, String> entry : keyValues.entrySet()) {
            ApiConfig config = makeApiConfig(entry.getKey(), entry.getValue());
            configList.addItemsItem(config);
        }
        return configList;
    }

    public static ApiConfig makeApiConfig(String name, String value) {
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setName(name);
        apiConfig.setValue(value);
        return apiConfig;
    }
}
