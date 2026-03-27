package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase.HbaseRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@ExtendWith(MockitoExtension.class)
class AtlasRdsConfigProviderTest {

    private static final String ATLAS_SERVER_COMPONENT = "ATLAS_SERVER";

    private static final String IDBROKER_COMPONENT = "IDBROKER";

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @InjectMocks
    private AtlasRdsConfigProvider underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "atlasDbUser", "atlas");
        ReflectionTestUtils.setField(underTest, "atlasDb", "atlas");
        ReflectionTestUtils.setField(underTest, "atlasDbPort", "5432");
    }

    @Test
    void testGetRdsType() {
        assertThat(underTest.getRdsType()).isEqualTo(DatabaseType.ATLAS);
    }

    @Test
    void testGetDbUser() {
        assertThat(underTest.getDbUser()).isEqualTo("atlas");
    }

    @Test
    void testGetDb() {
        assertThat(underTest.getDb()).isEqualTo("atlas");
    }

    @Test
    void testGetDbPort() {
        assertThat(underTest.getDbPort()).isEqualTo("5432");
    }

    @Test
    void testGetPillarKey() {
        assertThat(underTest.getPillarKey()).isEqualTo("atlas");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("isRdsConfigNeededArguments")
    void testIsRdsConfigNeeded(String name, boolean atlasPresent, boolean idbrokerPresent, boolean hbasePresent, boolean hdfsPresent, boolean expected) {
        Blueprint blueprint = mock(Blueprint.class);
        String blueprintText = "blueprintText";
        when(blueprint.getBlueprintJsonText()).thenReturn(blueprintText);

        CmTemplateProcessor processor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(blueprintText)).thenReturn(processor);

        when(processor.doesCMComponentExistsInBlueprint(ATLAS_SERVER_COMPONENT)).thenReturn(atlasPresent);
        lenient().when(processor.doesCMComponentExistsInBlueprint(IDBROKER_COMPONENT)).thenReturn(idbrokerPresent);
        lenient().when(processor.isServiceTypePresent(HbaseRoles.HBASE)).thenReturn(hbasePresent);
        lenient().when(processor.isServiceTypePresent(HdfsRoles.HDFS)).thenReturn(hdfsPresent);

        assertThat(underTest.isRdsConfigNeeded(blueprint, false)).isEqualTo(expected);
    }

    private static Stream<Arguments> isRdsConfigNeededArguments() {
        return Stream.of(
                Arguments.of("All conditions met", true, true, false, false, true),
                Arguments.of("Atlas server missing", false, true, false, false, false),
                Arguments.of("IDBroker missing", true, false, false, false, false),
                Arguments.of("HBase present", true, true, true, false, false),
                Arguments.of("HDFS present", true, true, false, true, false)
        );
    }
}