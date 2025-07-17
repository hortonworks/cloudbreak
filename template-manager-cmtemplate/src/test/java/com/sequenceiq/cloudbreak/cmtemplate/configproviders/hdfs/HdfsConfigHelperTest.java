package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.template.TemplateEndpoint;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;

class HdfsConfigHelperTest {

    private final HdfsConfigHelper underTest = new HdfsConfigHelper();

    @Test
    void testHdfsEndpointNullForHybrid() {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        when(templateProcessor.isHybridDatahub(source)).thenReturn(true);
        Set<TemplateEndpoint> endpoints = Set.of(new TemplateEndpoint(HdfsRoles.HDFS, HdfsRoles.NAMENODE, null));
        RdcView rdcView = new RdcView("crn", "rdc", endpoints, Set.of(), Set.of());
        when(source.getDatalakeView()).thenReturn(Optional.of(new DatalakeView(false, "crn", false, rdcView)));

        Optional<String> result = underTest.getAttachedDatalakeHdfsUrlForHybridDatahub(templateProcessor, source);

        assertTrue(result.isEmpty());
    }

}