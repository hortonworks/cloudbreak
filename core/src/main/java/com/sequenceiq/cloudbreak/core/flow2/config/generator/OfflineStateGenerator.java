package com.sequenceiq.cloudbreak.core.flow2.config.generator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.transition.Transition;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleConfig;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.FlowStructuredEventHandler;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.StructuredFlowEventFactory;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.ha.NodeConfig;

public class OfflineStateGenerator {

    private static final String INDEX_HTML_TEMPLATE = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
            "<title>Title</title>\n" +
            "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/bulma/0.7.5/css/bulma.min.css\">\n" +
            "<script defer src=\"https://use.fontawesome.com/releases/v5.3.1/js/all.js\"></script>\n" +
            "<script src=\"https://d3js.org/d3.v4.min.js\"></script>\n" +
            "<script src=\"https://unpkg.com/viz.js@1.8.1/viz.js\" type=\"javascript/worker\"></script>\n" +
            "<script src=\"https://unpkg.com/d3-graphviz@2.6.1/build/d3-graphviz.js\"></script>\n" +
            "<style>.graph { height: 800px; } .graph svg { height: 100%; width: 1500px; font-family: monospace }</style>" +
            "</head>\n" +
            "<body>\n" +
            "<h1 class=\"container title has-text-black is-1\">Cloudbreak Flow</h1>\n" +
            "<section class=\"section\"><div class=\"container\"><ul>TABLE_OF_CONTENT</ul></div></section>" +
            "FLOW_CHART_DIVS\n" +
            "<script>\n" +
            "GRAPH_SCRIPTS\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>";

    private static final String OUT_PATH = "build/diagrams/flow";

    private static final List<FlowConfiguration<? extends FlowEvent>> CONFIGS =
            Arrays.asList(
                    new ChangePrimaryGatewayFlowConfig(),
                    new ClusterCreationFlowConfig(),
                    new ClusterCredentialChangeFlowConfig(),
                    new ClusterDownscaleFlowConfig(),
                    new ClusterResetFlowConfig(),
                    new ClusterStartFlowConfig(),
                    new ClusterStopFlowConfig(),
                    new ClusterSyncFlowConfig(),
                    new ClusterTerminationFlowConfig(),
                    new ClusterUpgradeFlowConfig(),
                    new ClusterUpscaleFlowConfig(),
                    new EphemeralClusterFlowConfig(),
                    new InstanceTerminationFlowConfig(),
                    new ManualStackRepairTriggerFlowConfig(),
                    new StackCreationFlowConfig(),
                    new StackDownscaleConfig(),
                    new StackStartFlowConfig(),
                    new StackStopFlowConfig(),
                    new StackSyncFlowConfig(),
                    new StackTerminationFlowConfig(),
                    new StackUpscaleConfig(),
                    new MaintenanceModeValidationFlowConfig()
            );

    private static final ConfigurableApplicationContext APP_CONTEXT = new CustomApplicationContext();

    static {
        APP_CONTEXT.refresh();
    }

    private final FlowConfiguration<?> flowConfiguration;

    private OfflineStateGenerator(FlowConfiguration<?> flowConfiguration) {
        this.flowConfiguration = flowConfiguration;
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> graphsByFlowConfig = new LinkedHashMap<>();
        for (FlowConfiguration<?> flowConfiguration : CONFIGS) {
            graphsByFlowConfig.put(flowConfiguration.getClass().getSimpleName(), new OfflineStateGenerator(flowConfiguration).generate());
        }
        createSvgGraph(graphsByFlowConfig);
    }

    private static void createSvgGraph(Map<String, String> graphsByFlowConfig) throws IOException {
        String tableOfContent = graphsByFlowConfig
                .keySet()
                .stream()
                .map(flowConfigClassName -> "<li><a href=\"#" + flowConfigClassName + "Title\">" + flowConfigClassName + "</a></li>")
                .collect(Collectors.joining(System.lineSeparator()));
        String flowDivs = graphsByFlowConfig
                .keySet()
                .stream()
                .map(flowConfigClassName -> "<section class=\"section\"><div class=\"container is-fluid\"><h1 style=\"padding: 16px;\" id=\""
                        + flowConfigClassName + "Title\" class=\"title has-background-black has-text-white\">" + flowConfigClassName
                        + "</h1><div class=\"graph has-text-centered\" id=\"" + flowConfigClassName + "\"></div></div></section>")
                .collect(Collectors.joining(System.lineSeparator()));
        String flowScripts = graphsByFlowConfig
                .entrySet()
                .stream()
                .map(flowGraph -> "d3.select(\"#" + flowGraph.getKey() + "\").graphviz().fade(false).dot('"
                        + StringEscapeUtils.escapeJavaScript(flowGraph.getValue()) + "').fit(true).render();")
                .collect(Collectors.joining(System.lineSeparator()));
        String indexHtml = INDEX_HTML_TEMPLATE
                .replace("TABLE_OF_CONTENT", tableOfContent)
                .replace("FLOW_CHART_DIVS", flowDivs)
                .replace("GRAPH_SCRIPTS", flowScripts);
        Files.write(Paths.get(OUT_PATH, "index.html"), indexHtml.getBytes(StandardCharsets.UTF_8));
    }

    private String generate() throws Exception {
        StringBuilder graphBuild = new StringBuilder("digraph {\n");
        inject(flowConfiguration, "applicationContext", APP_CONTEXT);
        Flow flow = initializeFlow();
        StateMachine<FlowState, FlowEvent> stateMachine = getStateMachine(flow);
        FlowState init = stateMachine.getInitialState().getId();
        graphBuild.append(generateStartPoint(init, flowConfiguration.getClass().getSimpleName())).append('\n');
        List<Transition<FlowState, FlowEvent>> transitions = (List<Transition<FlowState, FlowEvent>>) stateMachine.getTransitions();
        Map<String, FlowState> transitionsAlreadyDefined = new HashMap<>();
        transitionsAlreadyDefined.put(init.toString(), init);
        while (!transitions.isEmpty()) {
            for (Transition<FlowState, FlowEvent> transition : new ArrayList<>(transitions)) {
                FlowState source = transition.getSource().getId();
                FlowState target = transition.getTarget().getId();
                if (transitionsAlreadyDefined.containsValue(source)) {
                    String id = generateTransitionId(source, target, transition.getTrigger().getEvent());
                    if (!transitionsAlreadyDefined.containsKey(id)) {
                        if (target.action() != null && !transitionsAlreadyDefined.containsValue(target)) {
                            graphBuild.append(generateState(target, target.action().getSimpleName())).append('\n');
                        }
                        graphBuild.append(generateTransition(source, target, transition.getTrigger().getEvent())).append('\n');
                        transitionsAlreadyDefined.put(id, target);
                    }
                    transitions.remove(transition);
                }
            }
        }
        String graph = graphBuild.append('}').toString();
        saveToFile(graph);
        return graph;
    }

    private String generateTransitionId(FlowState source, FlowState target, FlowEvent event) {
        return source.toString() + target.toString() + event.toString();
    }

    private String generateStartPoint(FlowState name, String label) {
        return String.format("%s [label=\"%s\" shape=ellipse color=green];", name, label);
    }

    private String generateState(FlowState state, String action) {
        return String.format("%s [label=\"%s\\n%s\" shape=rect color=black];", state, state, action);
    }

    private String generateTransition(FlowState source, FlowState target, FlowEvent event) {
        String color = "black";
        String style = "solid";
        if (Objects.equals(source, target)) {
            color = "blue";
        } else if (event.name().contains("FAIL") || event.name().contains("ERROR")) {
            color = "red";
            style = "dashed";
        }
        return String.format("%s -> %s [label=\"%s\" color=%s style=%s];", source, target, event, color, style);
    }

    private StateMachine<FlowState, FlowEvent> getStateMachine(Flow flow) throws NoSuchFieldException, IllegalAccessException {
        Field flowMachine = flow.getClass().getDeclaredField("flowMachine");
        flowMachine.setAccessible(true);
        return (StateMachine<FlowState, FlowEvent>) flowMachine.get(flow);
    }

    private Flow initializeFlow() throws Exception {
        ((AbstractFlowConfiguration<?, ?>) flowConfiguration).init();
        Flow flow = flowConfiguration.createFlow("", 0L);
        flow.initialize(Map.of());
        return flow;
    }

    private void saveToFile(String content) throws IOException {
        File destinationDir = new File(OUT_PATH);
        if (!destinationDir.exists()) {
            boolean success = destinationDir.mkdirs();
            if (!success) {
                throw new IOException("Unable to create directories: " + destinationDir.getAbsolutePath());
            }
        }
        Files.write(Paths.get(String.format("%s/%s.dot", OUT_PATH, flowConfiguration.getClass().getSimpleName())), content.getBytes(StandardCharsets.UTF_8));
    }

    private static void inject(Object target, String name, Object value) {
        Field field = null;
        try {
            field = target.getClass().getDeclaredField(name);
        } catch (NoSuchFieldException ignored) {
        }
        if (field == null) {
            try {
                field = target.getClass().getSuperclass().getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (NullPointerException | IllegalAccessException ignored) {
        }
    }

    static class CustomApplicationContext extends AbstractApplicationContext {

        @Override
        public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
            return (T) new CustomAction();
        }

        @Override
        protected void refreshBeanFactory() throws BeansException {
        }

        @Override
        protected void closeBeanFactory() {

        }

        @Override
        protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
            return new CustomBeanFactory();
        }

        @Override
        public ConfigurableListableBeanFactory getBeanFactory() {
            return obtainFreshBeanFactory();
        }
    }

    enum FlowStructuredEventHandlerParams {
        INIT_STATE, FINAL_STATE, FLOW_TYPE, FLOW_ID, STACK_ID
    }

    static class CustomBeanFactory extends DefaultListableBeanFactory {
        @Override
        public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
            FlowStructuredEventHandler<?, ?> bean = new FlowStructuredEventHandler(args[FlowStructuredEventHandlerParams.INIT_STATE.ordinal()],
                    args[FlowStructuredEventHandlerParams.FINAL_STATE.ordinal()], (String) args[FlowStructuredEventHandlerParams.FLOW_TYPE.ordinal()],
                    (String) args[FlowStructuredEventHandlerParams.FLOW_ID.ordinal()], (Long) args[FlowStructuredEventHandlerParams.STACK_ID.ordinal()]);

            inject(bean, "structuredEventClient", (StructuredEventClient) structuredEvent -> {
            });
            StructuredFlowEventFactory factory = new StructuredFlowEventFactory();
            inject(bean, "structuredFlowEventFactory", factory);
            inject(factory, "nodeConfig", new NodeConfig());
            inject(factory, "conversionService", new CustomConversionService());
            StackService stackService = new StackService();
            inject(factory, "stackService", stackService);
            inject(factory, "clock", new Clock());
            inject(stackService, "stackRepository", new CustomStackRepository());
            inject(stackService, "transactionService", new CustomTransactionService());

            return (T) bean;
        }
    }

    static class CustomStackRepository implements StackRepository {

        @Override
        public Optional<Stack> findByAmbari(String ambariIp) {
            return Optional.empty();
        }

        @Override
        public Set<Stack> findForWorkspaceIdWithLists(Long workspaceId) {
            return null;
        }

        @Override
        public Optional<Stack> findByNameAndWorkspaceId(String name, Long workspaceId) {
            return Optional.empty();
        }

        @Override
        public Optional<Stack> findByCrnAndWorkspaceId(String crn, Long workspaceId) {
            return Optional.empty();
        }

        @Override
        public Set<Stack> findByNameInAndWorkspaceId(Set<String> name, Long workspaceId) {
            return null;
        }

        @Override
        public Optional<Stack> findByNameAndWorkspaceIdWithLists(String name, StackType type, Long workspaceId, Boolean showTerminated, Long terminatedAfter) {
            return Optional.empty();
        }

        @Override
        public List<Stack> findByEnvironmentCrnAndStackType(String environmentCrn, StackType type) {
            return null;
        }

        @Override
        public Optional<Stack> findByNameAndWorkspaceIdWithLists(String name, Long workspaceId, Boolean showTerminated, Long terminatedAfter) {
            return Optional.empty();
        }

        @Override
        public Optional<Stack> findByCrnAndWorkspaceIdWithLists(String crn, Long workspaceId, Boolean showTerminated, Long terminatedAfter) {
            return Optional.empty();
        }

        @Override
        public Optional<Stack> findByCrnAndWorkspaceIdWithLists(String crn, StackType type, Long workspaceId, Boolean showTerminated, Long terminatedAfter) {
            return Optional.empty();
        }

        @Override
        public Optional<Stack> findOneWithLists(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<Stack> findOneByCrnWithLists(String crn) {
            return Optional.empty();
        }

        @Override
        public Optional<Stack> findOneWithCluster(Long id) {
            return Optional.empty();
        }

        @Override
        public Set<Stack> findEphemeralClusters(Long id) {
            return null;
        }

        @Override
        public List<Stack> findAllStackForTemplate(Long id) {
            return null;
        }

        @Override
        public List<Object[]> findStackStatusesWithoutAuth(Set<Long> ids) {
            return null;
        }

        @Override
        public Optional<Stack> findStackForCluster(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<Stack> findByNameInWorkspaceWithLists(String name, Workspace workspace) {
            return Optional.empty();
        }

        @Override
        public List<Stack> findAllAlive() {
            return null;
        }

        @Override
        public Set<Stack> findAllAliveWithInstanceGroups() {
            return null;
        }

        @Override
        public Set<Stack> findAllAliveWithNoWorkspaceOrUser() {
            return null;
        }

        @Override
        public List<Stack> findAllAliveAndProvisioned() {
            return null;
        }

        @Override
        public Set<Stack> findAllForWorkspace(Long workspaceId) {
            return null;
        }

        @Override
        public List<Stack> findByStatuses(List<Status> statuses) {
            return null;
        }

        @Override
        public Set<AutoscaleStack> findAliveOnesWithAmbari() {
            return null;
        }

        @Override
        public Set<Stack> findByNetwork(Network network) {
            return null;
        }

        @Override
        public List<String> findNamesOfAliveOnesByWorkspaceAndEnvironment(Long workspaceId, String environmentCrn) {
            return null;
        }

        @Override
        public Set<Stack> findTerminatedByWorkspaceIdAndEnvironmentId(Long workspaceId, Long environmentId) {
            return null;
        }

        @Override
        public Long countAliveOnesByWorkspaceAndEnvironment(Long workspaceId, String environmentCrn) {
            return null;
        }

        @Override
        public Long findWorkspaceIdById(Long id) {
            return null;
        }

        @Override
        public Long findWorkspaceIdByCrn(String crn) {
            return null;
        }

        @Override
        public Set<String> findDatalakeStackNamesByWorkspaceAndEnvironment(Long workspaceId, String environmentCrn) {
            return null;
        }

        @Override
        public Set<String> findWorkloadStackNamesByWorkspaceAndEnvironment(Long workspaceId, String environmentCrn) {
            return null;
        }

        @Override
        public Optional<Workspace> findWorkspaceById(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<Workspace> findWorkspaceByCrn(String crn) {
            return Optional.empty();
        }

        @Override
        public Optional<Stack> findTemplateWithLists(Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<Stack> findByResourceCrn(String crn) {
            return Optional.empty();
        }

        @Override
        public <S extends Stack> S save(S entity) {
            return entity;
        }

        @Override
        public <S extends Stack> Iterable<S> saveAll(Iterable<S> entities) {
            return entities;
        }

        @Override
        public Optional<Stack> findById(Long aLong) {
            Stack stack = new Stack();
            Workspace workspace = new Workspace();
            workspace.setTenant(new Tenant());
            stack.setWorkspace(workspace);
            stack.setCreator(new User());
            return Optional.of(stack);
        }

        @Override
        public boolean existsById(Long aLong) {
            return false;
        }

        @Override
        public Iterable<Stack> findAll() {
            return Collections.emptyList();
        }

        @Override
        public Iterable<Stack> findAllById(Iterable<Long> longs) {
            return Collections.emptyList();
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public void deleteById(Long aLong) {

        }

        @Override
        public void delete(Stack entity) {

        }

        @Override
        public void deleteAll(Iterable<? extends Stack> entities) {

        }

        @Override
        public void deleteAll() {

        }

        @Override
        public Set<Stack> findAllByWorkspace(Workspace workspace) {
            return null;
        }

        @Override
        public Optional<Stack> findByNameAndWorkspace(String name, Workspace workspace) {
            return Optional.empty();
        }

        @Override
        public Set<Stack> findAllByWorkspaceId(Long workspaceId) {
            return null;
        }
    }

    static class CustomConversionService implements ConversionService {

        @Override
        public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
            return false;
        }

        @Override
        public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
            return false;
        }

        @Override
        public <T> T convert(Object source, Class<T> targetType) {
            return null;
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            return null;
        }
    }

    static class CustomAction extends AbstractStackAction<FlowState, FlowEvent, CommonContext, Payload> {
        CustomAction() {
            super(Payload.class);
        }

        @Override
        public void execute(StateContext<FlowState, FlowEvent> context) {
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, Payload payload) {
            return null;
        }

        @Override
        protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {

        }

        @Override
        protected Selectable createRequest(CommonContext context) {
            return null;
        }

        @Override
        protected Object getFailurePayload(Payload payload, Optional<CommonContext> flowContext, Exception ex) {
            return null;
        }
    }

    static class CustomTransactionService extends TransactionService {
        public <T> T required(Supplier<T> callback) {
            return callback.get();
        }
    }
}
