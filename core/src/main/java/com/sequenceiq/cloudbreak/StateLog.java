package com.sequenceiq.cloudbreak;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogIdFlowAndType;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.repository.FlowLogRepository;

import freemarker.template.Template;

public class StateLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateLog.class);

    private static final String LOG_FILE = "test-state-" + System.currentTimeMillis() + ".html";
    private static Template template;
    private static FlowLogRepository flowLogRepository;
    private static Queue<Traced> logs = new ConcurrentLinkedDeque<>();

    private static Thread writer = new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                LOGGER.debug("Persisting logs");
                String html = logs.stream()
                        .map(StateLog::render)
                        .collect(Collectors.joining(System.lineSeparator()));
                Files.write(Paths.get(LOG_FILE), html.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
                LOGGER.debug("Persisted logs");
                Thread.sleep(10000);
            } catch (Exception ex) {
                LOGGER.error("{}", ex.getMessage());
            }
        }
    });

    public static void init(FlowLogRepository flowLogRepository) {
        writer.start();
        StateLog.flowLogRepository = flowLogRepository;
    }

    public static void main(String[] args) throws InterruptedException {
        TestFlowLogRepository flowLogRepository = new TestFlowLogRepository();
        init(flowLogRepository);
        while (true) {
            double random = new Random().nextDouble();
            Stack stack = new Stack();
            stack.setId(1L);
            stack.setName("test-stack");
            stack.setStackStatus(new StackStatus(stack, Status.AVAILABLE, "Stack status reason", DetailedStackStatus.AVAILABLE));
            Cluster cluster = new Cluster();
            cluster.setId(1L);
            cluster.setName("test-cluster");
            cluster.setStatus(Status.AVAILABLE);
            cluster.setStatusReason("Cluster status reasson");
            HostMetadata hostMetadata = new HostMetadata();
            hostMetadata.setId(1L);
            hostMetadata.setHostName("host");
            hostMetadata.setHostMetadataState(HostMetadataState.HEALTHY);
            HostGroup hostGroup = new HostGroup();
            hostMetadata.setHostGroup(hostGroup);
            hostGroup.setHostMetadata(Set.of(hostMetadata));
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setId(1L);
            instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
            instanceMetaData.setDiscoveryFQDN("master");
            InstanceGroup instanceGroup = new InstanceGroup();
            instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
            instanceMetaData.setInstanceGroup(instanceGroup);
            if (random < 0.2) {
                logStackChange(stack);
            } else if (random < 0.4) {
                cluster.setStack(stack);
                logClusterChange(cluster);
            } else if (random < 0.6) {
                logClusterChange(cluster);
            } else if (random < 0.8) {
                cluster.setStack(stack);
                hostGroup.setCluster(cluster);
                logHostMetadataChange(hostMetadata);
            } else if (random < 1.0) {
                instanceGroup.setStack(stack);
                logInstanceMetadataChange(instanceMetaData);
            }
            Thread.sleep(5000);
        }
    }

    public static void logStackChange(Stack stack) {
        LOGGER.debug("Stack log: {}", stack);
        logs.add(new Traced(Thread.currentThread().getStackTrace(), toStackRecord(stack)));
    }

    public static void logClusterChange(Cluster cluster) {
        LOGGER.debug("Cluster log: {}", cluster);
        logs.add(new Traced(Thread.currentThread().getStackTrace(), toClusterRecord(cluster)));
    }

    public static void logHostMetadataChange(HostMetadata hostMetadata) {
        LOGGER.debug("HostMetadata log: {}", hostMetadata);
        logs.add(new Traced(Thread.currentThread().getStackTrace(), toHostMetadataRecord(hostMetadata)));
    }

    public static void logInstanceMetadataChange(InstanceMetaData instanceMetaData) {
        LOGGER.debug("InstanceMetaData log: {}", instanceMetaData);
        logs.add(new Traced(Thread.currentThread().getStackTrace(), toInstanceMetadataRecord(instanceMetaData)));
    }

    private static StackRecord toStackRecord(Stack stack) {
        StackRecord stackChange;
        if (stack.getId() != null) {
            stackChange = new StackRecord()
                    .pendingFlow(getPendingFlow(stack.getId()))
                    .stackId(stack.getId())
                    .name(stack.getName())
                    .stackStatus(stack.getStatus())
                    .detailedStackStatus(stack.getStackStatus().getDetailedStackStatus())
                    .reason(stack.getStatusReason());
        } else {
            stackChange = new StackRecord()
                    .pendingFlow(null)
                    .name(stack.getName())
                    .stackStatus(stack.getStatus())
                    .detailedStackStatus(stack.getStackStatus().getDetailedStackStatus())
                    .reason(stack.getStatusReason());
        }
        return stackChange;
    }

    private static ClusterRecord toClusterRecord(Cluster cluster) {
        ClusterRecord clusterRecord = new ClusterRecord()
                .clusterId(cluster.getId())
                .clusterName(cluster.getName())
                .clusterStatus(cluster.getStatus())
                .clusterStatusReason(cluster.getStatusReason());
        if (cluster.getStack() != null && cluster.getStack().getId() != null) {
            clusterRecord.stackRecord(toStackRecord(cluster.getStack()));
        }
        return clusterRecord;
    }

    private static HostMetadataRecord toHostMetadataRecord(HostMetadata hostMetadata) {
        HostMetadataRecord hostMetadataRecord = new HostMetadataRecord()
                .id(hostMetadata.getId())
                .hostName(hostMetadata.getHostName())
                .hostMetadataState(hostMetadata.getHostMetadataState());
        if (hostMetadata.getHostGroup() != null && hostMetadata.getHostGroup().getCluster() != null) {
            hostMetadataRecord.clusterRecord(toClusterRecord(hostMetadata.getHostGroup().getCluster()));
        }
        return hostMetadataRecord;
    }

    private static InstanceMetadataRecord toInstanceMetadataRecord(InstanceMetaData instanceMetaData) {
        InstanceMetadataRecord instanceMetadataRecord = new InstanceMetadataRecord()
                .id(instanceMetaData.getId())
                .discoveryFQDN(instanceMetaData.getDiscoveryFQDN())
                .instanceStatus(instanceMetaData.getInstanceStatus());
        if (instanceMetaData.getInstanceGroup() != null && instanceMetaData.getInstanceGroup().getStack() != null) {
            instanceMetadataRecord.stackRecord(toStackRecord(instanceMetaData.getInstanceGroup().getStack()));
        }
        return instanceMetadataRecord;
    }

    private static FlowLog getPendingFlow(Long stackId) {
        return flowLogRepository
                .findAllByResourceIdOrderByCreatedDesc(stackId)
                .stream()
                .filter(f -> StateStatus.PENDING.equals(f.getStateStatus()))
                .findFirst()
                .orElse(null);
    }

    private static String render(Traced traced) {
        String innerContent = "";
        String type = "";
        if (StackRecord.class.equals(traced.log.getClass())) {
            type = "Stack";
            StackRecord stackRecord = (StackRecord) traced.log;
            innerContent = "<p class=\"status\"><code>status -> " + orNoData(stackRecord.stackStatus) + "<br/>detailedStackStatus -> " + orNoData(stackRecord.detailedStackStatus) + "</code></p>" +
                    getFlowInformation(stackRecord.pendingFlow) +
                    "<div class=\"detail\">" + getStackInfo(stackRecord) +
                    "</div>";
        } else if (ClusterRecord.class.equals(traced.log.getClass())) {
            type = "Cluster";
            ClusterRecord clusterRecord = (ClusterRecord) traced.log;
            StackRecord stackRecord = clusterRecord.stackRecord;
            FlowLog pendingFlow = stackRecord == null ? null : stackRecord.pendingFlow;
            innerContent = "<p class=\"status\"><code>status ->" + orNoData(clusterRecord.clusterStatus) + "</code></p>" +
                    getFlowInformation(pendingFlow) +
                    "<div class=\"detail\">" + getClusterInfo(clusterRecord) + getStackInfo(stackRecord) +
                    "</div>";
        } else if (HostMetadataRecord.class.equals(traced.log.getClass())) {
            HostMetadataRecord hostMetadataRecord = (HostMetadataRecord) traced.log;
            ClusterRecord clusterRecord = hostMetadataRecord.clusterRecord;
            StackRecord stackRecord = clusterRecord == null ? null : clusterRecord.stackRecord;
            FlowLog pendingFlow = stackRecord == null ? null : stackRecord.pendingFlow;
            innerContent = "<p class=\"status\"><code>hostMetadataState ->" + orNoData(hostMetadataRecord.hostMetadataState) + "</code></p>" +
                    getFlowInformation(pendingFlow) +
                    "<div class=\"detail\">" + getHostMetadataInfo(hostMetadataRecord) + getClusterInfo(clusterRecord) + getStackInfo(stackRecord) +
                    "</div>";
            type = "Host meta data";
        } else if (InstanceMetadataRecord.class.equals(traced.log.getClass())) {
            InstanceMetadataRecord instanceMetadataRecord = (InstanceMetadataRecord) traced.log;
            StackRecord stackRecord = instanceMetadataRecord.stackRecord;
            FlowLog pendingFlow = stackRecord == null ? null : stackRecord.pendingFlow;
            innerContent = "<p class=\"status\"><code>instanceStatus ->" + orNoData(instanceMetadataRecord.instanceStatus) + "</code></p>" +
                    getFlowInformation(pendingFlow) +
                    "<div class=\"detail\">" + getInstanceMetadataInfo(instanceMetadataRecord) + getStackInfo(stackRecord) +
                    "</div>";
            type = "Instance meta data";
        }
        return "<div class=\"record\"><p class=\"type\">" + type + "</p><p class=\"created\">" + traced.log.getCreated() + "</p>" + innerContent
                + "<div class=\"stacktrace\"><pre>" + Stream.of(traced.stack)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining(System.lineSeparator()))
                + "</pre></div></div>";
    }

    private static String getStackInfo(StackRecord stackRecord) {
        return "<p class=\"title\">Stack:</p>" +
                (stackRecord == null ? "<p>N/A</p>" :
                        "<p>Id: " + orNoData(stackRecord.stackId) + "</p>" +
                                "<p>Name: " + orNoData(stackRecord.name) + "</p>" +
                                "<p>Status: " + orNoData(stackRecord.stackStatus) + "</p>" +
                                "<p>Detailed: " + orNoData(stackRecord.detailedStackStatus) + "</p>" +
                                "<p>Reason: " + orNoData(stackRecord.reason) + "</p>");
    }

    private static String getClusterInfo(ClusterRecord clusterRecord) {
        return "<p class=\"title\">Cluster:</p>" +
                (clusterRecord == null ? "<p>N/A</p>" :
                        "<p>Id: " + orNoData(clusterRecord.clusterId) + "</p>" +
                                "<p>Name: " + orNoData(clusterRecord.clusterName) + "</p>" +
                                "<p>Status: " + orNoData(clusterRecord.clusterStatus) + "</p>" +
                                "<p>Reason: " + orNoData(clusterRecord.clusterStatusReason) + "</p>");
    }

    private static String getHostMetadataInfo(HostMetadataRecord hostMetadataRecord) {
        return "<p class=\"title\">HostMetadata:</p>" +
                (hostMetadataRecord == null ? "<p>N/A</p>" :
                        "<p>Id: " + orNoData(hostMetadataRecord.id) + "</p>" +
                                "<p>Name: " + orNoData(hostMetadataRecord.hostName) + "</p>" +
                                "<p>HostMetadataState: " + orNoData(hostMetadataRecord.hostMetadataState) + "</p>");
    }

    private static String getInstanceMetadataInfo(InstanceMetadataRecord instanceMetadataRecord) {
        return "<p class=\"title\">InstanceMetadata:</p>" +
                (instanceMetadataRecord == null ? "<p>N/A</p>" :
                        "<p>Id: " + orNoData(instanceMetadataRecord.id) + "</p>" +
                                "<p>discoveryFQDN: " + orNoData(instanceMetadataRecord.discoveryFQDN) + "</p>" +
                                "<p>InstanceStatus: " + orNoData(instanceMetadataRecord.instanceStatus) + "</p>");
    }

    private static String getFlowInformation(FlowLog pendingFlow) {
        return "<p class=\"flow\"><code>" + (pendingFlow == null ? orNoData(null) : pendingFlow.getFlowType() + " -> " + pendingFlow.getCurrentState()) + "</code></p>";
    }

    private static String orNoData(Object o) {
        return o == null ? "N/A" : o.toString();
    }

    static class Traced {
        private StackTraceElement[] stack;
        private Created log;

        Traced(StackTraceElement[] stack, Created log) {
            this.stack = stack;
            this.log = log;
        }
    }

    interface Created {
        long getCreated();
    }

    static class StackRecord implements Created {
        private long created = System.currentTimeMillis();
        private Long stackId;
        private String name;
        private Status stackStatus;
        private DetailedStackStatus detailedStackStatus;
        private String reason;
        private FlowLog pendingFlow;

        @Override
        public long getCreated() {
            return created;
        }

        public StackRecord pendingFlow(FlowLog pendingFlow) {
            this.pendingFlow = pendingFlow;
            return this;
        }

        public StackRecord stackId(Long stackId) {
            this.stackId = stackId;
            return this;
        }

        public StackRecord name(String name) {
            this.name = name;
            return this;
        }

        public StackRecord stackStatus(Status stackStatus) {
            this.stackStatus = stackStatus;
            return this;
        }

        public StackRecord detailedStackStatus(DetailedStackStatus detailedStackStatus) {
            this.detailedStackStatus = detailedStackStatus;
            return this;
        }

        public StackRecord reason(String reason) {
            this.reason = reason;
            return this;
        }
    }

    static class ClusterRecord implements Created {
        private long created = System.currentTimeMillis();
        private Long clusterId;
        private String clusterName;
        private Status clusterStatus;
        private String clusterStatusReason;
        private StackRecord stackRecord;

        @Override
        public long getCreated() {
            return created;
        }

        public ClusterRecord clusterId(Long clusterId) {
            this.clusterId = clusterId;
            return this;
        }

        public ClusterRecord clusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public ClusterRecord clusterStatus(Status clusterStatus) {
            this.clusterStatus = clusterStatus;
            return this;
        }

        public ClusterRecord clusterStatusReason(String clusterStatusReason) {
            this.clusterStatusReason = clusterStatusReason;
            return this;
        }

        public ClusterRecord stackRecord(StackRecord stackRecord) {
            this.stackRecord = stackRecord;
            return this;
        }
    }

    static class HostMetadataRecord implements Created {
        private long created = System.currentTimeMillis();
        private Long id;
        private String hostName;
        private HostMetadataState hostMetadataState;
        private ClusterRecord clusterRecord;

        @Override
        public long getCreated() {
            return created;
        }

        public HostMetadataRecord id(Long id) {
            this.id = id;
            return this;
        }

        public HostMetadataRecord hostName(String hostName) {
            this.hostName = hostName;
            return this;
        }

        public HostMetadataRecord hostMetadataState(HostMetadataState hostMetadataState) {
            this.hostMetadataState = hostMetadataState;
            return this;
        }

        public HostMetadataRecord clusterRecord(ClusterRecord clusterRecord) {
            this.clusterRecord = clusterRecord;
            return this;
        }
    }

    static class InstanceMetadataRecord implements Created {
        private long created = System.currentTimeMillis();
        private Long id;
        private String discoveryFQDN;
        private com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus instanceStatus;
        private StackRecord stackRecord;

        @Override
        public long getCreated() {
            return created;
        }

        public InstanceMetadataRecord id(Long id) {
            this.id = id;
            return this;
        }

        public InstanceMetadataRecord discoveryFQDN(String discoveryFQDN) {
            this.discoveryFQDN = discoveryFQDN;
            return this;
        }


        public InstanceMetadataRecord stackRecord(StackRecord stackRecord) {
            this.stackRecord = stackRecord;
            return this;
        }

        public InstanceMetadataRecord instanceStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus instanceStatus) {
            this.instanceStatus = instanceStatus;
            return this;
        }
    }

    static class TestFlowLogRepository implements FlowLogRepository {

        @Override
        public Optional<FlowLog> findFirstByFlowIdOrderByCreatedDesc(String flowId) {
            return Optional.empty();
        }

        @Override
        public Set<FlowLogIdFlowAndType> findAllRunningFlowLogByResourceId(Long resourceId) {
            return null;
        }

        @Override
        public List<Object[]> findAllPending() {
            return null;
        }

        @Override
        public void finalizeByFlowId(String flowId) {

        }

        @Override
        public Set<FlowLog> findAllByCloudbreakNodeId(String cloudbreakNodeId) {
            return null;
        }

        @Override
        public Set<FlowLog> findAllUnassigned() {
            return null;
        }

        @Override
        public void updateLastLogStatusInFlow(Long id, StateStatus stateStatus) {

        }

        @Override
        public List<FlowLog> findAllByResourceIdOrderByCreatedDesc(Long resourceId) {
            FlowLog flowLog = new FlowLog();
            flowLog.setFlowType(StateLog.class);
            flowLog.setCurrentState("TEST_STATE");
            return List.of(flowLog);
        }

        @Override
        public List<FlowLog> findAllByFlowIdOrderByCreatedDesc(String flowId) {
            return null;
        }

        @Override
        public <S extends FlowLog> S save(S entity) {
            return null;
        }

        @Override
        public <S extends FlowLog> Iterable<S> saveAll(Iterable<S> entities) {
            return null;
        }

        @Override
        public Optional<FlowLog> findById(Long aLong) {
            return Optional.empty();
        }

        @Override
        public boolean existsById(Long aLong) {
            return false;
        }

        @Override
        public Iterable<FlowLog> findAll() {
            return null;
        }

        @Override
        public Iterable<FlowLog> findAllById(Iterable<Long> longs) {
            return null;
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public void deleteById(Long aLong) {

        }

        @Override
        public void delete(FlowLog entity) {

        }

        @Override
        public void deleteAll(Iterable<? extends FlowLog> entities) {

        }

        @Override
        public void deleteAll() {

        }
    }
}
