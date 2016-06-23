package com.sequenceiq.cloudbreak.orchestrator.salt.poller

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

import org.junit.Assert
import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse

class BaseSaltJobRunnerTest {

    private var targets: MutableSet<String> = HashSet()

    private var baseSaltJobRunner: BaseSaltJobRunner? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        targets = HashSet<String>()
        targets.add("10.0.0.1")
        targets.add("10.0.0.2")
        targets.add("10.0.0.3")
    }

    @Test
    @Throws(Exception::class)
    fun collectMissingNodesAllNodeAndSaltNodeWithPrefix() {
        val allNode = allNodeWithPostFix()
        baseSaltJobRunner = object : BaseSaltJobRunner(targets, allNode) {
            override fun submit(saltConnector: SaltConnector): String {
                return ""
            }
        }

        val returnedNodes = HashSet<String>()
        returnedNodes.add("host-10-0-0-1.example.com")
        returnedNodes.add("host-10-0-0-2.example.com")
        val collectedMissingNodes = baseSaltJobRunner!!.collectMissingNodes(returnedNodes)

        Assert.assertEquals(1, collectedMissingNodes.size.toLong())
        Assert.assertEquals("10.0.0.3", collectedMissingNodes.iterator().next())
    }

    @Test
    @Throws(Exception::class)
    fun collectMissingNodesAllNodeWithoutPrefixSaltNodeWithPrefix() {
        val allNode = allNodeWithoutPostFix()
        baseSaltJobRunner = object : BaseSaltJobRunner(targets, allNode) {
            override fun submit(saltConnector: SaltConnector): String {
                return ""
            }
        }

        val returnedNodes = HashSet<String>()
        returnedNodes.add("host-10-0-0-1.example.com")
        returnedNodes.add("host-10-0-0-2.example.com")
        val collectedMissingNodes = baseSaltJobRunner!!.collectMissingNodes(returnedNodes)

        Assert.assertEquals(1, collectedMissingNodes.size.toLong())
        Assert.assertEquals("10.0.0.3", collectedMissingNodes.iterator().next())
    }

    @Test
    @Throws(Exception::class)
    fun collectMissingNodesAllNodeAndSaltNodeWithoutPrefix() {
        val allNode = allNodeWithoutPostFix()
        baseSaltJobRunner = object : BaseSaltJobRunner(targets, allNode) {
            override fun submit(saltConnector: SaltConnector): String {
                return ""
            }
        }

        val returnedNodes = HashSet<String>()
        returnedNodes.add("host-10-0-0-1")
        returnedNodes.add("host-10-0-0-2")
        val collectedMissingNodes = baseSaltJobRunner!!.collectMissingNodes(returnedNodes)

        Assert.assertEquals(1, collectedMissingNodes.size.toLong())
        Assert.assertEquals("10.0.0.3", collectedMissingNodes.iterator().next())
    }

    @Test
    @Throws(Exception::class)
    fun collectMissingNodesAllNodeWithPrefixAndSaltNodeWithoutPrefix() {
        val allNode = allNodeWithoutPostFix()
        baseSaltJobRunner = object : BaseSaltJobRunner(targets, allNode) {
            override fun submit(saltConnector: SaltConnector): String {
                return ""
            }
        }

        val returnedNodes = HashSet<String>()
        returnedNodes.add("host-10-0-0-1.example.com")
        returnedNodes.add("host-10-0-0-2.example.com")
        val collectedMissingNodes = baseSaltJobRunner!!.collectMissingNodes(returnedNodes)

        Assert.assertEquals(1, collectedMissingNodes.size.toLong())
        Assert.assertEquals("10.0.0.3", collectedMissingNodes.iterator().next())
    }

    @Test
    @Throws(Exception::class)
    fun collectNodesTest() {
        val allNode = allNodeWithPostFix()
        baseSaltJobRunner = object : BaseSaltJobRunner(targets, allNode) {
            override fun submit(saltConnector: SaltConnector): String {
                return ""
            }
        }
        val applyResponse = ApplyResponse()
        val resultList = ArrayList<Map<String, Any>>()
        val resultMap = HashMap<String, Any>()
        resultMap.put("host-10-0-0-1.example.com", "10.0.0.1")
        resultMap.put("host-10-0-0-2.example.com", "10.0.0.2")
        resultMap.put("host-10-0-0-3.example.com", "10.0.0.3")
        resultList.add(resultMap)
        applyResponse.result = resultList
        val collectedNodes = baseSaltJobRunner!!.collectNodes(applyResponse)
        Assert.assertEquals(3, collectedNodes.size.toLong())
        Assert.assertTrue(collectedNodes.contains("host-10-0-0-1.example.com"))
        Assert.assertTrue(collectedNodes.contains("host-10-0-0-2.example.com"))
        Assert.assertTrue(collectedNodes.contains("host-10-0-0-3.example.com"))
    }

    private fun allNodeWithPostFix(): Set<Node> {
        val allNode = HashSet<Node>()
        for (i in 1..3) {
            val node = Node("10.0.0." + i, "88.77.66.5" + i, "host-10-0-0-$i.example.com")
            allNode.add(node)
        }
        return allNode
    }


    private fun allNodeWithoutPostFix(): Set<Node> {
        val allNode = HashSet<Node>()
        for (i in 1..3) {
            val node = Node("10.0.0." + i, "88.77.66.5" + i, "host-10-0-0-" + i)
            allNode.add(node)
        }
        return allNode
    }
}