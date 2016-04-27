package com.sequenceiq.cloudbreak.orchestrator.onhost.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.onhost.domain.CbBootResponse;
import com.sequenceiq.cloudbreak.orchestrator.onhost.domain.CbBootResponses;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class OnHostClient {

    private enum OnHostClientEndpoint {
        INFO("health"),
        AMBARI_RUN_DISTRIBUTE("ambari/run/distribute"),
        CONSUL_RUN_DISTRIBUTE("consul/run/distribute"),
        CONSUL_CONFIG_DISTRIBUTE("consul/config/distribute");

        private String url;

        OnHostClientEndpoint(String url) {
            this.url = url;
        }

        public String url() {
            return url;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(OnHostClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    private String gatewayPublicIp;
    private String gatewayPrivateIp;
    private Set<String> targets = new HashSet<>();
    private String port;

    public OnHostClient(String gatewayPublicIp, String gatewayPrivateIp, Set<String> targets, String port) {
        this.gatewayPrivateIp = gatewayPrivateIp;
        this.gatewayPublicIp = gatewayPublicIp;
        this.targets = targets;
        this.port = port;
    }

    public OnHostClient(String gatewayPublicIp, String gatewayPrivateIp, String port) {
        this.gatewayPrivateIp = gatewayPrivateIp;
        this.gatewayPublicIp = gatewayPublicIp;
        this.port = port;
    }

    public Set<String> getTargets() {
        return targets;
    }

    private HttpHeaders httpHeaders() {
        String plainCreds = "cbadmin:cbadmin";
        byte[] plainCredsBytes = plainCreds.getBytes();
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Creds);
        return headers;
    }

    private HttpEntity httpEntity(Map<String, Object> config) throws JsonProcessingException {
        return new HttpEntity<>(JsonUtil.writeValueAsString(config), httpHeaders());
    }

    private <T extends Class> ResponseEntity exchange(Map<String, Object> entity, HttpMethod method, String port,
            OnHostClientEndpoint onHostClientEndpoint, T clazz) throws Exception {
        String endpoint = String.format("http://%s:%s/cbboot/%s", gatewayPublicIp, port, onHostClientEndpoint.url());
        return restTemplate.exchange(endpoint, method, httpEntity(entity), clazz);
    }

    private <T extends Class> ResponseEntity exchange(HttpMethod method, String port, OnHostClientEndpoint onHostClientEndpoint, T clazz) throws Exception {
        return exchange(new HashMap<String, Object>(), method, port, onHostClientEndpoint, clazz);
    }


    public boolean info() {
        try {
            ResponseEntity<String> response = exchange(HttpMethod.GET, port, OnHostClientEndpoint.INFO, String.class);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public Set<String> distributeConsulConfig(Set<String> targetIps) throws CloudbreakOrchestratorException {
        Set<String> missingTargets = new HashSet<>();
        // TODO replace it with object
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("data_dir", "/etc/cloudbreak/consul");
        // TODO multiple servers
        configMap.put("servers", Arrays.asList(gatewayPrivateIp));
        configMap.put("targets", targetIps);
        try {
            LOGGER.info("Sending consul config save request to {}", targetIps);
            ResponseEntity<CbBootResponses> response = exchange(configMap, HttpMethod.POST, port, OnHostClientEndpoint.CONSUL_CONFIG_DISTRIBUTE,
                    CbBootResponses.class);
            CbBootResponses responseBody = response.getBody();
            for (CbBootResponse cbBootResponse : responseBody.getResponses()) {
                if (cbBootResponse.getStatusCode() != HttpStatus.OK.value()) {
                    LOGGER.info("Successfully distributed consul config to: " + cbBootResponse.getAddress());
                    missingTargets.add(cbBootResponse.getAddress().split(":")[0]);
                }
            }
            if (!missingTargets.isEmpty()) {
                LOGGER.info("Missing nodes to save consul config: {}", missingTargets);
            }
        } catch (Exception e) {
            throw new CloudbreakOrchestratorFailedException(e);
        }
        return missingTargets;
    }

    public Set<String> startAmbariServiceOnTargetMachines(Set<String> targetIps) throws CloudbreakOrchestratorException {
        Set<String> missingTargets = new HashSet<>();
        Map<String, Object> map = new HashMap<>();
        Set<String> agents = new HashSet<>(targets);
        if (agents.contains(gatewayPrivateIp)) {
            map.put("server", gatewayPrivateIp);
        }
        agents.remove(gatewayPrivateIp);
        map.put("agents", agents);
        try {
            ResponseEntity<CbBootResponses> response = exchange(map, HttpMethod.POST, port, OnHostClientEndpoint.AMBARI_RUN_DISTRIBUTE, CbBootResponses.class);
            CbBootResponses responseBody = response.getBody();
            LOGGER.info("Ambari run response: {}", responseBody);
            for (CbBootResponse cbBootResponse : responseBody.getResponses()) {
                if (cbBootResponse.getStatusCode() != HttpStatus.OK.value()) {
                    LOGGER.info("Successfully distributed ambari run to: " + cbBootResponse.getAddress());
                    missingTargets.add(cbBootResponse.getAddress().split(":")[0]);
                }
            }
            if (!missingTargets.isEmpty()) {
                LOGGER.info("Missing nodes to run ambari: %s", missingTargets);
            }
        } catch (Exception e) {
            LOGGER.info("Error occured when ran ambari on hosts: ", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        return missingTargets;
    }


    public Set<String> startConsulServiceOnTargetMachines(Set<String> targetIps) throws CloudbreakOrchestratorException {
        Set<String> missingTargets = new HashSet<>();
        try {
            Map<String, Object> consulRunMap = new HashMap<>();
            consulRunMap.put("targets", targetIps);
            ResponseEntity<CbBootResponses> response =
                    exchange(consulRunMap, HttpMethod.POST, port, OnHostClientEndpoint.CONSUL_RUN_DISTRIBUTE, CbBootResponses.class);
            CbBootResponses responseBody = response.getBody();
            LOGGER.info("Consul run response: {}", responseBody);
            for (CbBootResponse cbBootResponse : responseBody.getResponses()) {
                if (cbBootResponse.getStatusCode() != HttpStatus.OK.value()) {
                    LOGGER.info("Successfully distributed consul run to: " + cbBootResponse.getAddress());
                    missingTargets.add(cbBootResponse.getAddress().split(":")[0]);
                }
            }
            if (!missingTargets.isEmpty()) {
                LOGGER.info("Missing nodes to run consul: %s", missingTargets);
            }
        } catch (Exception e) {
            LOGGER.info("Error occured when ran consul on hosts: ", e);
            throw new CloudbreakOrchestratorFailedException(e);
        }
        return missingTargets;
    }

}
