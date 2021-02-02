package com.sequenceiq.cloudbreak.cm.commands;

import org.springframework.stereotype.Service;

@Service
public class ActiveCommandTableResource extends AbstractCommandTableResource {

    @Override
    public String getUriPath() {
        return "/cmf/commands/activeCommandTable";
    }
}