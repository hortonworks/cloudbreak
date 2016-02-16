package com.sequenceiq.cloudbreak.orchestrator.marathon;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.orchestrator.ContainerBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.SimpleContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.marathon.poller.MarathonAppBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.marathon.poller.MarathonAppDeletion;
import com.sequenceiq.cloudbreak.orchestrator.marathon.poller.MarathonTaskDeletion;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.Container;
import mesosphere.marathon.client.model.v2.Docker;
import mesosphere.marathon.client.model.v2.GetAppResponse;
import mesosphere.marathon.client.model.v2.Task;
import mesosphere.marathon.client.utils.MarathonException;

@Component
public class MarathonContainerOrchestrator extends SimpleContainerOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarathonContainerOrchestrator.class);
    private static final double MIN_CPU = 0.5;
    private static final int MIN_MEM = 1024;
    private static final int MIN_INSTANCES = 1;
    private static final String HOST_NETWORK_MODE = "HOST";
    private static final String DOCKER_CONTAINER_TYPE = "DOCKER";
    private static final String SPACE = " ";
    private static final Integer STATUS_NOT_FOUND = 404;
    private static final int LENGTH_OF_RANDOM_SUFFIX_CHARS = 4;


    @Override
    public void validateApiEndpoint(OrchestrationCredential cred) throws CloudbreakOrchestratorException {
        Marathon client = MarathonClient.getInstance(cred.getApiEndpoint());
        try {
            client.getServerInfo();
        } catch (Exception e) {
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public List<ContainerInfo> runContainer(ContainerConfig config, OrchestrationCredential cred, ContainerConstraint constraint,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {

        String image = config.getName() + ":" + config.getVersion();
        String name = constraint.getContainerName().getName().replace("_", "-");
        String appName = constraint.getAppName();

        try {
            List<ContainerInfo> result = new ArrayList<>();
            Marathon client = MarathonClient.getInstance(cred.getApiEndpoint());
            App app;
            if (appName == null) {
                app = createMarathonApp(config, constraint, image, name);
                app = postAppToMarathon(config, client, app);
            } else {
                app = getMarathonApp(client, constraint.getAppName());
                app.setInstances(app.getTasksRunning() + constraint.getInstances());
                updateApp(client, createMarathonUpdateApp(app));
            }

            MarathonAppBootstrap bootstrap = new MarathonAppBootstrap(client, app);
            Callable<Boolean> runner = runner(bootstrap, getExitCriteria(), exitCriteriaModel);
            Future<Boolean> appFuture = getParallelContainerRunner().submit(runner);
            appFuture.get();

            App appResponse = client.getApp(app.getId()).getApp();
            for (Task task : appResponse.getTasks()) {
                if (!isTaskFound(app, task)) {
                    result.add(new ContainerInfo(task.getId(), appResponse.getId(), task.getHost(), image));
                }
            }
            return result;
        } catch (Exception ex) {
            //To provide that the failed Marathon app and its deployment are not deleted from Marathon
            deleteContainer(Arrays.asList(new ContainerInfo(name, name, "", image)), cred);
            String msg = String.format("Failed to create marathon app from image: '%s', with name: '%s'.", image, name);
            throw new CloudbreakOrchestratorFailedException(msg, ex);
        }
    }

    @Override
    public void startContainer(List<ContainerInfo> info, OrchestrationCredential cred) throws CloudbreakOrchestratorException {

    }

    @Override
    public void stopContainer(List<ContainerInfo> info, OrchestrationCredential cred) throws CloudbreakOrchestratorException {

    }

    @Override
    public void deleteContainer(List<ContainerInfo> containerInfo, OrchestrationCredential cred) throws CloudbreakOrchestratorException {
        try {
            Marathon client = MarathonClient.getInstance(cred.getApiEndpoint());
            List<Future<Boolean>> futures = new ArrayList<>();

            Map<String, Set<String>> containersPerApp = new HashMap<>();
            for (ContainerInfo info : containerInfo) {
                if (!containersPerApp.containsKey(info.getName())) {
                    containersPerApp.put(info.getName(), Sets.newHashSet(info.getId()));
                } else {
                    containersPerApp.get(info.getName()).add(info.getId());
                }
            }

            for (String appName : containersPerApp.keySet()) {
                App app = client.getApp(appName).getApp();
                Set<String> tasksInApp = FluentIterable.from(app.getTasks()).transform(new Function<Task, String>() {
                    @Override
                    public String apply(Task input) {
                        return input.getId();
                    }
                }).toSet();
                if (containersPerApp.get(appName).containsAll(tasksInApp)) {
                    deleteEntireApp(client, futures, appName);
                } else {
                    deleteTasksFromApp(client, futures, containersPerApp, appName);
                }
            }

            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception ex) {
            String appNames = Arrays.toString(FluentIterable.from(containerInfo).transform(new Function<ContainerInfo, String>() {
                @Override
                public String apply(ContainerInfo input) {
                    return input.getName();
                }
            }).toArray(String.class));
            String msg = String.format("Failed to delete Marathon app with app ids: '%s'.", appNames);
            throw new CloudbreakOrchestratorFailedException(msg, ex);
        }
    }

    @Override
    public boolean areAllNodesAvailable(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return false;
    }

    @Override
    public List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return null;
    }

    @Override
    public String name() {
        return "MARATHON";
    }

    @Override
    public void bootstrap(GatewayConfig gatewayConfig, ContainerConfig config, Set<Node> nodes, int consulServerCount, String consulLogLocation,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {

    }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, ContainerConfig containerConfig, Set<Node> nodes, String consulLogLocation,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {

    }

    @Override
    public boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig) {
        return false;
    }

    @Override
    public int getMaxBootstrapNodes() {
        return 0;
    }

    private App createMarathonApp(ContainerConfig config, ContainerConstraint constraint, String image, String name) {
        App app = new App();
        app.setId(name);
        app.setCpus(constraint.getCpu() != null ? constraint.getCpu() : MIN_CPU);
        app.setMem(constraint.getMem() != null ? constraint.getMem() : MIN_MEM);
        app.setInstances(constraint.getInstances() != null ? constraint.getInstances() : MIN_INSTANCES);
        app.setEnv(constraint.getEnv());

        String[] arrayOfCmd = constraint.getCmd();
        if (arrayOfCmd != null && arrayOfCmd.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("/usr/sbin/init");
            for (String cmd : arrayOfCmd) {
                sb.append(SPACE);
                sb.append(cmd);
            }
            app.setCmd(sb.toString());
        }

        for (Integer port : constraint.getPorts()) {
            app.addPort(port);
        }

        Docker docker = new Docker();
        docker.setPrivileged(true);
        docker.setImage(image);
        docker.setNetwork(HOST_NETWORK_MODE);

        Container container = new Container();
        container.setType(DOCKER_CONTAINER_TYPE);
        container.setDocker(docker);
        app.setContainer(container);
        return app;
    }

    private void deleteEntireApp(Marathon client, List<Future<Boolean>> futures, String appName) throws CloudbreakOrchestratorFailedException {
        try {
            client.deleteApp(appName);
            MarathonAppDeletion appDeletion = new MarathonAppDeletion(client, appName);
            Callable<Boolean> runner = runner(appDeletion, getExitCriteria(), null);
            futures.add(getParallelContainerRunner().submit(runner));
        } catch (MarathonException me) {
            if (STATUS_NOT_FOUND.equals(me.getStatus())) {
                LOGGER.info("Marathon app '{}' has already been deleted.", appName);
            } else {
                throw new CloudbreakOrchestratorFailedException(me);
            }
        }
    }

    private void deleteTasksFromApp(Marathon client, List<Future<Boolean>> futures, Map<String, Set<String>> containersPerApp, String appName)
            throws CloudbreakOrchestratorFailedException {
        Set<String> taskIds = containersPerApp.get(appName);
        for (String taskId : taskIds) {
            try {
                client.deleteAppTask(appName, taskId, "true");
                MarathonTaskDeletion taskDeletion = new MarathonTaskDeletion(client, appName, taskIds);
                Callable<Boolean> runner = runner(taskDeletion, getExitCriteria(), null);
                futures.add(getParallelContainerRunner().submit(runner));
            } catch (MarathonException me) {
                if (STATUS_NOT_FOUND.equals(me.getStatus())) {
                    LOGGER.info("Marathon task '{}' has already been deleted from app '{}'.", taskId, appName);
                } else {
                    throw new CloudbreakOrchestratorFailedException(me);
                }
            }
        }
    }

    private App getMarathonApp(Marathon client, String appId) throws CloudbreakOrchestratorFailedException {
        try {
            GetAppResponse resp = client.getApp(appId);
            return resp.getApp();
        } catch (MarathonException e) {
            String msg = String.format("Failed to get Marathon app: %s", appId);
            LOGGER.error(msg, e);
            throw new CloudbreakOrchestratorFailedException(msg, e);
        }
    }

    private App createMarathonUpdateApp(App appResponse) {
        App app = new App();
        app.setId(appResponse.getId());
        app.setInstances(appResponse.getInstances());
        return app;
    }

    private App postAppToMarathon(ContainerConfig config, Marathon client, App app) throws CloudbreakOrchestratorFailedException {
        try {
            return client.createApp(app);
        } catch (MarathonException e) {
            String msg = String.format("Marathon container creation failed. From image: '%s', with name: '%s'!", config.getName(), app.getId());
            LOGGER.error(msg, e);
            throw new CloudbreakOrchestratorFailedException(msg, e);
        }
    }

    private void updateApp(Marathon client, App app) throws CloudbreakOrchestratorFailedException {
        try {
            client.updateApp(app.getId(), app);
        } catch (MarathonException e) {
            String msg = String.format("Failed to scale Marathon app %s to %s instances!", app.getId(), app.getInstances());
            LOGGER.error(msg, e);
            throw new CloudbreakOrchestratorFailedException(msg, e);
        }
    }

    private boolean isTaskFound(App app, Task task) {
        boolean taskFound = false;
        if (app.getTasks() != null) {
            for (Task oldTask : app.getTasks()) {
                if (oldTask.getId().equals(task.getId())) {
                    taskFound = true;
                    break;
                }
            }
        }
        return taskFound;
    }

    private Callable<Boolean> runner(ContainerBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel) {
        return new ContainerBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap());
    }
}
