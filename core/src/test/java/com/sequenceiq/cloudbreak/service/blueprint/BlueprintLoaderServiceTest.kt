package com.sequenceiq.cloudbreak.service.blueprint

import org.mockito.Matchers.any
import org.mockito.Matchers.anyString
import org.mockito.Mockito.`when`

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.HashSet

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.runners.MockitoJUnitRunner
import org.mockito.stubbing.Answer
import org.springframework.core.convert.ConversionService
import org.springframework.test.util.ReflectionTestUtils

import com.sequenceiq.cloudbreak.api.model.BlueprintRequest
import com.sequenceiq.cloudbreak.controller.json.JsonHelper
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.repository.BlueprintRepository
import com.sequenceiq.cloudbreak.util.JsonUtil

@RunWith(MockitoJUnitRunner::class)
class BlueprintLoaderServiceTest {

    @InjectMocks
    private val underTest: BlueprintLoaderService? = null

    @Mock
    private val conversionService: ConversionService? = null

    @Mock
    private val blueprintRepository: BlueprintRepository? = null

    @Mock
    private val jsonHelper: JsonHelper? = null

    @Before
    @Throws(IOException::class)
    fun setUp() {
        `when`(conversionService!!.convert<Any>(any<BlueprintRequest>(BlueprintRequest::class.java), any<Class>(Class<Any>::class.java))).thenAnswer { invocation ->
            val args = invocation.arguments
            val blueprintRequest = args[0] as BlueprintRequest
            blueprint(blueprintRequest)
        }
        `when`(blueprintRepository!!.findAllDefaultInAccount(anyString())).thenReturn(HashSet<Blueprint>())
        `when`(blueprintRepository.save(any<Blueprint>(Blueprint::class.java))).thenReturn(Blueprint())
        `when`(jsonHelper!!.createJsonFromString(anyString())).thenReturn(JsonUtil.readTree("{\"Blueprints\":{\"blueprint_name\":\"hdp-etl-edw-tp\","
                + "\"stack_name\":\"HDP\",\"stack_version\":\"2.5\"},\"configurations\":[{\"core-site\":{\"fs.trash.interval\":\"4320\"}},{\"hdfs-site\":"
                + "{\"dfs.namenode.safemode.threshold-pct\":\"0.99\"}},{\"hive-site\":{\"hive.exec.compress.output\":\"true\",\"hive.merge.mapfiles\""
                + ":\"true\",\"hive.server2.tez.initialize.default.sessions\":\"true\"}},{\"mapred-site\":{\"mapreduce.job.reduce.slowstart.completedmaps\""
                + ":\"0.7\",\"mapreduce.map.output.compress\":\"true\",\"mapreduce.output.fileoutputformat.compress\":\"true\"}},{\"yarn-site\""
                + ":{\"yarn.acl.enable\":\"true\"}}],\"host_groups\":[{\"name\":\"master\",\"configurations\":[],\"components\":[{\"name\""
                + ":\"APP_TIMELINE_SERVER\"},{\"name\":\"HCAT\"},{\"name\":\"HDFS_CLIENT\"},{\"name\":\"HISTORYSERVER\"},{\"name\":\"HIVE_CLIENT\"}"
                + ",{\"name\":\"HIVE_METASTORE\"},{\"name\":\"HIVE_SERVER\"},{\"name\":\"JOURNALNODE\"},{\"name\":\"LIVY_SERVER\"},{\"name\""
                + ":\"MAPREDUCE2_CLIENT\"},{\"name\":\"METRICS_COLLECTOR\"},{\"name\":\"METRICS_GRAFANA\"},{\"name\":\"METRICS_MONITOR\"},"
                + "{\"name\":\"MYSQL_SERVER\"},{\"name\":\"NAMENODE\"},{\"name\":\"PIG\"},{\"name\":\"RESOURCEMANAGER\"},{\"name\":\"SECONDARY_NAMENODE\"}"
                + ",{\"name\":\"SPARK2_CLIENT\"},{\"name\":\"SPARK2_JOBHISTORYSERVER\"},{\"name\":\"SQOOP\"},{\"name\":\"TEZ_CLIENT\"},"
                + "{\"name\":\"WEBHCAT_SERVER\"},{\"name\":\"YARN_CLIENT\"},{\"name\":\"ZEPPELIN_MASTER\"},{\"name\":\"ZOOKEEPER_CLIENT\"},"
                + "{\"name\":\"ZOOKEEPER_SERVER\"}],\"cardinality\":\"1\"},{\"name\":\"worker\",\"configurations\":[],\"components\":["
                + "{\"name\":\"DATANODE\"},{\"name\":\"METRICS_MONITOR\"},{\"name\":\"NODEMANAGER\"}],\"cardinality\":\"1+\"}]}"))
    }

    @Test
    fun threeDefaultBlueprintWithCorrectParameters() {
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("test1=testloader", "test2=testloader", "test3=testloader"))
        val blueprints = underTest!!.loadBlueprints(cbUser())
        Assert.assertEquals(3, blueprints.size.toLong())
    }

    @Test
    fun twoDefaultBlueprintWithCorrectParameters() {
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("test1=testloader", "test2=testloader"))
        val blueprints = underTest!!.loadBlueprints(cbUser())
        Assert.assertEquals(2, blueprints.size.toLong())
    }

    @Test
    fun threeDefaultBlueprintButOneIsIncorrect() {
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("test1=testloader", "test2=testloader", "incorrect"))
        val blueprints = underTest!!.loadBlueprints(cbUser())
        Assert.assertEquals(2, blueprints.size.toLong())
    }

    @Test
    fun threeDefaultBlueprintButEveryParamIsInCorrect() {
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("incorrect0", "incorrect1", "incorrect2"))
        val blueprints = underTest!!.loadBlueprints(cbUser())
        Assert.assertEquals(0, blueprints.size.toLong())
    }

    @Test
    fun threeDefaultBlueprintButEveryParamIsJustFileName() {
        ReflectionTestUtils.setField(underTest, "blueprintArray", Arrays.asList("testloader", "testloader", "testloader"))
        val blueprints = underTest!!.loadBlueprints(cbUser())
        Assert.assertEquals(3, blueprints.size.toLong())
    }

    @Test
    fun launchDefaultBlueprintEveryParamIsCorrect() {
        ReflectionTestUtils.setField(underTest, "blueprintArray",
                Arrays.asList(
                        "EDW-ETL: Apache Hive 1.2.1, Apache Spark 1.6=testloader",
                        "Data Science: Apache Spark 1.6, Zeppelin=testloader",
                        "EDW-ETL: Apache Hive 1.2.1, Apache Spark 1.6=testloader",
                        "EDW-ETL: Apache Spark 2.0-preview, Apache Hive 2.0=testloader",
                        "EDW-Analytics: Apache Hive 2.0 LLAP, Apache Zeppelin=testloader"))
        val blueprints = underTest!!.loadBlueprints(cbUser())
        Assert.assertEquals(5, blueprints.size.toLong())
    }

    private fun blueprint(blueprintRequest: BlueprintRequest): Blueprint {
        val blueprint = Blueprint()
        blueprint.name = blueprintRequest.name
        blueprint.blueprintText = blueprintRequest.ambariBlueprint
        return blueprint
    }

    private fun cbUser(): CbUser {
        return CbUser("userId", "userName", "account", ArrayList<CbUserRole>(), "givenName", "familyName", Date())
    }
}
