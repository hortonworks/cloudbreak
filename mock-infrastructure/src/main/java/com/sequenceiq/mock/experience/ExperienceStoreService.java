package com.sequenceiq.mock.experience;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.sequenceiq.mock.experience.response.common.CpInternalCluster;
import com.sequenceiq.mock.experience.response.common.CpInternalEnvironmentResponse;

@Service
public class ExperienceStoreService {

    private final Map<String, CpInternalEnvironmentResponse> store = new ConcurrentHashMap<>();

    private final AtomicLong idCounter = new AtomicLong();

    private String createID() {
        return String.valueOf(idCounter.getAndIncrement());
    }

    public CpInternalEnvironmentResponse get(String env) {
        return store.get(env);
    }

    public void deleteById(String env) {
        CpInternalEnvironmentResponse exp = store.get(env);
        if (exp != null) {
            exp.getResults().forEach(experience -> experience.setStatus("DELETED"));
        }
    }

    public void createIfNotExist(String env) {
        CpInternalEnvironmentResponse clusters = new CpInternalEnvironmentResponse();
        clusters.setResults(new HashSet<>());
        if (!store.containsKey(env)) {
            store.put(env, clusters);
        }
    }

    public void addExperienceTo(String env) {
        CpInternalCluster cluster = new CpInternalCluster();
        cluster.setStatus("ALIVEANDKICKING");
        cluster.setName(createID());
        cluster.setCrn(env);
        createIfNotExist(env);
        store.get(env).getResults().add(cluster);
    }

    public CpInternalCluster put(String id, CpInternalCluster cluster) {
        store.values().stream()
                .flatMap(response -> response.getResults().stream())
                .filter(experience -> id.equals(experience.getName()))
                .forEach(experience -> {
                    experience.setStatus(cluster.getStatus());
                    experience.setCrn(cluster.getCrn());
                });
        return cluster;
    }

    public CpInternalCluster changeStatusById(String id, String status) {
        Optional<CpInternalCluster> res = store.values().stream()
                .flatMap(response -> response.getResults().stream())
                .filter(experience -> id.equals(experience.getName()))
                .findFirst();
        res.ifPresent(experience -> experience.setStatus(status));
        return res.orElse(new CpInternalCluster());
    }
}
