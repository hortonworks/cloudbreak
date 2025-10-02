package com.sequenceiq.cloudbreak.cm.error.mapper;

import java.util.List;

import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public interface ClouderaManagerErrorMapper {

    boolean canHandle(StackDtoDelegate stack, List<CommandDetails> failedCommands);

    String map(StackDtoDelegate stack, List<CommandDetails> failedCommands, String originalMessage);
}
