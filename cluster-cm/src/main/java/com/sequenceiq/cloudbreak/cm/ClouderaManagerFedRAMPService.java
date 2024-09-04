package com.sequenceiq.cloudbreak.cm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiConfigEnforcement;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class ClouderaManagerFedRAMPService {

    public static final String LOGIN_BANNER = "LOGIN_BANNER";

    private String banner;

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException, IOException {
        banner = FileReaderUtils.readFileFromClasspath("banner.txt");
    }

    public List<ApiConfigEnforcement> getApiConfigEnforcements() {
        List<ApiConfigEnforcement> apiConfigEnforcements = new ArrayList<>();

        ApiConfigEnforcement loginBannerEnforcement = new ApiConfigEnforcement();
        loginBannerEnforcement.setLabel(LOGIN_BANNER);
        loginBannerEnforcement.setDefaultValue(banner);

        apiConfigEnforcements.add(loginBannerEnforcement);
        return apiConfigEnforcements;
    }
}
