package com.sequenceiq.cloudbreak.orchestrator.swarm.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;

public class BindsBuilder {

    private List<Bind> binds;

    public BindsBuilder() {
        binds = new ArrayList<>();
    }

    public BindsBuilder addDockerSocket() {
        return add("/var/run/docker.sock");
    }

    public BindsBuilder addDockerSocket(String containerPath) {
        return add("/var/run/docker.sock", containerPath);
    }

    public BindsBuilder addLog() {
        return add("/hadoopfs/fs1/logs", "/var/log");
    }

    public BindsBuilder addLog(String... subdirs) {
        for (String subdir : subdirs) {
            add("/hadoopfs/fs1/logs/" + subdir, "/var/log/containers/" + subdir);
        }
        return this;
    }

    public BindsBuilder add(String... paths) {
        for (String path : paths) {
            add(path);
        }
        return this;
    }


    public BindsBuilder add(Collection<String> paths) {
        for (String path : paths) {
            add(path);
        }
        return this;
    }

    public BindsBuilder add(String path) {
        return add(path, path);
    }

    public BindsBuilder add(String hostPath, String containerPath) {
        Bind bind = new Bind(hostPath, new Volume(containerPath));
        binds.add(bind);
        return this;
    }

    public Bind[] build() {
        return binds.toArray(new Bind[binds.size()]);
    }
}
