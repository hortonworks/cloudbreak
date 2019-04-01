package com.sequenceiq.cloudbreak.orchestrator.yarn.util;

import static com.sequenceiq.cloudbreak.orchestrator.yarn.api.ComponentType.AMBARIAGENT;
import static com.sequenceiq.cloudbreak.orchestrator.yarn.api.ComponentType.AMBARIDB;
import static com.sequenceiq.cloudbreak.orchestrator.yarn.api.ComponentType.AMBARISERVER;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.yarn.api.ComponentType;

@Service
public class ApplicationUtils {

    private static final String AMBARI_DB_PREFIX = "ambari_db";

    private static final String AMBARI_SERVER_PREFIX = "ambari-server";

    private static final String AMBARI_AGENT_PREFIX = "ambari-agent";

    @Value("${cb.yarn.domain:}")
    private String yarnDomain;

    /**
     * Return the application name based on the container name in the constraint.
     *
     * This is less than ideal as each containerName follows a different pattern.
     *
     * ambari_db-<cluster name> -> return <cluster name>-ambaridb
     * ambari-server-<cluster name> -> return <cluster name>-ambariserver
     * ambari-agent%<cluster name>%<host group> -> return <cluster name>-<host group>-<componentNumber>
     *
     * @param constraint - ContainerConstraint used to create the application.
     * @param componentNumber - This number uniquely identifies the component with the application.
     * @return the application name to use.
     * @throws CloudbreakOrchestratorFailedException - if the ComponentType is unknown.
     */
    public String getApplicationName(ContainerConstraint constraint, int componentNumber) throws CloudbreakOrchestratorFailedException {
        String containerName = constraint.getContainerName().getName();
        switch (getComponentType(constraint)) {
            case AMBARIDB:
                return String.format("%s-%s", containerName.replace("ambari_db-", "").replaceAll("_", "-"), getComponentName(constraint, componentNumber));
            case AMBARISERVER:
                return String.format("%s-%s", containerName.replace("ambari-server-", "").replaceAll("_", "-"), getComponentName(constraint, componentNumber));
            case AMBARIAGENT:
                String[] containerNameParts = constraint.getContainerName().getName().split("%");
                return String.format("%s-%s", containerNameParts[1].replace("_", "-"), getComponentName(constraint, componentNumber));
            default:
                throw new CloudbreakOrchestratorFailedException("Unknown component type");
        }
    }

    /**
     * Return the component name based on the component type and  in the constraint.
     *
     * This is less than ideal as each containerName follows a different pattern.
     *
     * ambari_db-<cluster name> -> return ambaridb
     * ambari-server-<cluster name> -> return ambariserver
     * ambari-agent%<cluster name>%<host group> -> return <host group>-<componentNumber>
     *
     * Because Ambari Blueprints allow for hyphens,
     *
     * @param constraint - ContainerConstraint used to create the application.
     * @param componentNumber - This number uniquely identifies the component with the application.
     * @return the component name to use.
     * @throws CloudbreakOrchestratorFailedException - if the ComponentType is unknown.
     */
    public String getComponentName(ContainerConstraint constraint, int componentNumber) throws CloudbreakOrchestratorFailedException {
        switch (getComponentType(constraint)) {
            case AMBARIDB:
                return "ambaridb";
            case AMBARISERVER:
                return "ambariserver";
            case AMBARIAGENT:
                String[] containerNameParts = constraint.getContainerName().getName().split("%");
                String componentBaseName = containerNameParts[2].substring(0, containerNameParts[2].lastIndexOf('-')).replaceAll("[^a-zA-Z0-9 ]", "");
                return String.format("%s-%s", componentBaseName, componentNumber);
            default:
                throw new CloudbreakOrchestratorFailedException("Unknown component type");
        }
    }

    /**
     * Return the fully qualified hostname for the component.
     *
     * @param constraint - ContainerConstraint used to create the application.
     * @param cred - OrchestrationCredential associated with the application.
     * @param componentNumber - This number uniquely identifies the component with the application.
     * @return the fully qualified hostname for the component.
     */
    public String getComponentHostName(ContainerConstraint constraint, OrchestrationCredential cred, int componentNumber)
            throws CloudbreakOrchestratorFailedException {
        String component = getComponentName(constraint, componentNumber);
        String application = getApplicationName(constraint, componentNumber);
        return String.format("%s.%s.%s", component, application, yarnDomain);
    }

    /**
     * Return the ComponentType based on the constraint container name.
     *
     * @param constraint - ContainerConstraint used to create the application.
     * @return - the ComponentType associated with the container name.
     */
    public ComponentType getComponentType(ContainerConstraint constraint) throws CloudbreakOrchestratorFailedException {
        String componentName = constraint.getContainerName().getName();
        if (componentName.startsWith(AMBARI_DB_PREFIX)) {
            return AMBARIDB;
        } else if (componentName.startsWith(AMBARI_SERVER_PREFIX)) {
            return AMBARISERVER;
        } else if (componentName.startsWith(AMBARI_AGENT_PREFIX)) {
            return AMBARIAGENT;
        }
        throw new CloudbreakOrchestratorFailedException("Unknown component type.");
    }
}
