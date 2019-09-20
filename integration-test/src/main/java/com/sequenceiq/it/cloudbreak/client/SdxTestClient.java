package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCreateAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxCreateInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDeleteAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDeleteInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDescribeAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDescribeInternalAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxListAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRefreshAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxRepairAction;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxSyncAction;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxRepairTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;

@Service
public class SdxTestClient {

    public Action<SdxTestDto, SdxClient> create() {
        return new SdxCreateAction();
    }

    public Action<SdxInternalTestDto, SdxClient> createInternal() {
        return new SdxCreateInternalAction();
    }

    public Action<SdxTestDto, SdxClient> delete() {
        return new SdxDeleteAction();
    }

    public Action<SdxInternalTestDto, SdxClient> deleteInternal() {
        return new SdxDeleteInternalAction();
    }

    public Action<SdxTestDto, SdxClient> describe() {
        return new SdxDescribeAction();
    }

    public Action<SdxInternalTestDto, SdxClient> describeInternal() {
        return new SdxDescribeInternalAction();
    }

    public Action<SdxTestDto, SdxClient> list() {
        return new SdxListAction();
    }

    public Action<SdxTestDto, SdxClient> sync() {
        return new SdxSyncAction();
    }

    public Action<SdxTestDto, SdxClient> refresh() {
        return new SdxRefreshAction();
    }

    public Action<SdxRepairTestDto, SdxClient> repair() {
        return new SdxRepairAction();
    }
}
