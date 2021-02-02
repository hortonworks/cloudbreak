package com.sequenceiq.cloudbreak.cm.commands;

import org.springframework.stereotype.Service;

@Service
public class RecentCommandTableResource extends AbstractCommandTableResource {

    @Override
    public String getUriPath() {
        return "/cmf/commands/commandTable";
    }
}