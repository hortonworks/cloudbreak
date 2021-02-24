package com.sequenceiq.mock.experience;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.mock.experience.response.liftie.LiftieClusterView;
import com.sequenceiq.mock.experience.response.liftie.ListClustersResponse;
import com.sequenceiq.mock.experience.response.liftie.PageStats;
import com.sequenceiq.mock.experience.response.liftie.StatusMessage;

@Service
public class LiftieExperienceStoreService {
    private final Map<String, LiftieClusterView> store = new ConcurrentHashMap<>();

    private final AtomicLong idCounter = new AtomicLong();

    private String createID() {
        return String.valueOf(idCounter.getAndIncrement());
    }

    public void create(String env, String tenant) {
        String id = "liftie" + createID();
        StatusMessage clusterStatus = new StatusMessage();
        clusterStatus.setMessage("");
        clusterStatus.setStatus("RUNNING");
        store.put(id, new LiftieClusterView(id, id, env, tenant, "X", clusterStatus));
    }

    public void createIfNotExist(String env, String tenant) {
        if (store.values().stream().noneMatch(cluster -> env.equals(cluster.getEnv()))) {
            create(env, tenant);
        }
    }

    public void deleteById(String id) {
        setStatusById(id, "DELETED");
    }

    public LiftieClusterView setStatusById(String id, String status) {
        LiftieClusterView cluster = store.get(id);
        if (cluster != null) {
            cluster.getClusterStatus().setStatus(status);
        }
        return cluster;
    }

    public ListClustersResponse get(String env) {
        Map<String, LiftieClusterView> clusters = store.values().stream()
                .filter(cluster -> env.equals(cluster.getEnv()))
                .collect(Collectors.toMap(LiftieClusterView::getClusterId, Function.identity()));
        return create(clusters);
    }

    private ListClustersResponse create(Map<String, LiftieClusterView> clusters) {
        ListClustersResponse listClustersResponse = new ListClustersResponse();
        listClustersResponse.setClusters(clusters);
        PageStats pageStat = new PageStats();
        pageStat.setTotalElements(1);
        pageStat.setTotalPages(1);
        pageStat.setNumber(1);
        pageStat.setSize(clusters.size());
        listClustersResponse.setPage(pageStat);
        return listClustersResponse;
    }

    public LiftieClusterView getById(String id) {
        return store.get(id);
    }

    public LiftieClusterView changeById(String id, LiftieClusterView liftieClusterView) {
        liftieClusterView.setClusterId(id);
        liftieClusterView.setName(id);
        store.put(id, liftieClusterView);
        return liftieClusterView;
    }
}
