package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType.POST_UPGRADE_CLUSTER;
import static com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType.UPGRADE_CLUSTER;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;

public enum ClouderaManagerCommand {

    RUNTIME_UPGRADE_COMMAND("UpgradeCluster", UPGRADE_CLUSTER),
    POST_RUNTIME_UPGRADE_COMMAND("PostClouderaRuntimeUpgradeCommand", POST_UPGRADE_CLUSTER);

    private static final Map<ClusterCommandType, ClouderaManagerCommand> COMMANDS_BY_TYPE;

    static {
        COMMANDS_BY_TYPE = Stream.of(ClouderaManagerCommand.values())
                .collect(Collectors.toMap(ClouderaManagerCommand::getType, Function.identity()));
    }

    private final String name;

    private final ClusterCommandType type;

    ClouderaManagerCommand(String name, ClusterCommandType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public ClusterCommandType getType() {
        return type;
    }

    public static ClouderaManagerCommand ofType(ClusterCommandType type) {
        return COMMANDS_BY_TYPE.get(type);
    }
}
