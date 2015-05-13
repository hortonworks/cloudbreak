package com.sequenceiq.cloudbreak.service.stack.connector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class UserDataBuilder {

    private String userDataScripts;

    public void setUserDataScripts(String userDataScripts) {
        this.userDataScripts = userDataScripts;
    }

    @PostConstruct
    public void readUserDataScript() throws IOException {
        userDataScripts = FileReaderUtils.readFileFromClasspath("init/init.sh");
    }

    public String buildUserData(CloudPlatform cloudPlatform) {
        Map<String, Object> model = new HashMap<>();
        model.put("platform_disk_prefix", cloudPlatform.getDiskPrefix());
        model.put("platform_disk_start_label", cloudPlatform.startLabel());
        String result = userDataScripts;
        for (Map.Entry<String, Object> param : model.entrySet()) {
            result = result.replaceAll(param.getKey(), param.getValue().toString());
        }
        return result;
    }
}
