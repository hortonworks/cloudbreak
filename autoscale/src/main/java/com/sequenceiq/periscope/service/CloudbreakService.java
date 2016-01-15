package com.sequenceiq.periscope.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint;
import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.periscope.service.security.TokenService;

@Service
public class CloudbreakService {

    @Autowired
    private TokenService tokenService;

    @Autowired
    @Qualifier("cloudbreakUrl")
    private String cloudbreakUrl;


    private <T extends Object> T endPointFactory(Class<T> clazz) throws Exception {
        String token = tokenService.getToken();
        JAXRSClientFactoryBean jaxrsClientFactoryBean = jaxrsClientFactoryBean(token);
        jaxrsClientFactoryBean.setResourceClass(clazz);
        Object clientFactory = JAXRSClientFactory.fromClient(jaxrsClientFactoryBean.create(), clazz, true);
        return (T) clientFactory;
    }


    private JAXRSClientFactoryBean jaxrsClientFactoryBean(String token) {
        JAXRSClientFactoryBean jaxrsClientFactoryBean = new JAXRSClientFactoryBean();
        String addressWithoutLastSlash = cloudbreakUrl.endsWith("/") ? cloudbreakUrl.substring(0, cloudbreakUrl.length() - 1) : cloudbreakUrl;
        String apiAddress = addressWithoutLastSlash + CoreApi.API_ROOT_CONTEXT;
        jaxrsClientFactoryBean.setAddress(apiAddress);
        jaxrsClientFactoryBean.setProvider(JacksonJsonProvider.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        jaxrsClientFactoryBean.setHeaders(headers);
        return jaxrsClientFactoryBean;
    }

    public StackEndpoint stackEndpoint() throws Exception {
        return  endPointFactory(StackEndpoint.class);
    }

    public ClusterEndpoint clusterEndpoint() throws Exception {
        return  endPointFactory(ClusterEndpoint.class);
    }
}
