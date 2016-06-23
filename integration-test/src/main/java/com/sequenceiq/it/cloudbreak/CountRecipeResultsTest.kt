package com.sequenceiq.it.cloudbreak

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Arrays
import java.util.Properties
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

import org.springframework.beans.factory.annotation.Value
import org.testng.Assert
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.it.IntegrationTestContext

class CountRecipeResultsTest : AbstractCloudbreakIntegrationTest() {

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private val defaultPrivateKeyFile: String? = null

    @Value("${integrationtest.ambariContainer}")
    private val ambariContainer: String? = null

    @Test
    @Parameters("searchRecipesOnHosts", "lookingFor", "require")
    @Throws(Exception::class)
    fun testFetchRecipeResult(searchRecipesOnHosts: String, lookingFor: String, require: Int?) {
        Assert.assertEquals(File(defaultPrivateKeyFile).exists(), true, "Private cert file not found: " + defaultPrivateKeyFile!!)
        Assert.assertFalse(lookingFor.isEmpty())

        val itContext = itContext
        val stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID)
        val stackEndpoint = itContext.getContextParam<CloudbreakClient>(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient::class.java).stackEndpoint()
        val instanceGroups = stackEndpoint[java.lang.Long.valueOf(stackId)].instanceGroups
        val files = lookingFor.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        val publicIps = getPublicIps(instanceGroups, Arrays.asList<String>(*searchRecipesOnHosts.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()))
        val futures = ArrayList<Future<Any>>(publicIps.size * files.size)
        val executorService = Executors.newFixedThreadPool(publicIps.size)
        val count = AtomicInteger(0)

        try {
            for (file in files) {
                for (ip in publicIps) {
                    futures.add(executorService.submit {
                        if (findFile(ip, file)!!) {
                            count.incrementAndGet()
                        }
                    })
                }
            }

            for (future in futures) {
                future.get()
            }
        } finally {
            executorService.shutdown()
        }

        Assert.assertEquals(count.get(), require!!.toInt(), "The number of existing files is different than required.")
    }

    private fun getPublicIps(instanceGroups: List<InstanceGroupJson>, hostGroupsWithRecipe: List<String>): List<String> {
        val ips = ArrayList<String>()
        for (instanceGroup in instanceGroups) {
            if (hostGroupsWithRecipe.contains(instanceGroup.group)) {
                for (metaData in instanceGroup.metadata) {
                    ips.add(metaData.publicIp)
                }
            }
        }
        return ips
    }

    private fun findFile(host: String, file: String): Boolean? {
        var resp: Boolean? = false
        var session: Session? = null
        var executor: ChannelExec? = null
        try {
            val jsch = JSch()
            jsch.addIdentity(defaultPrivateKeyFile)

            val config = Properties()
            config.put("StrictHostKeyChecking", "no")

            session = jsch.getSession("cloudbreak", host, 22)
            session!!.setConfig(config)
            session.connect(10000)

            executor = session.openChannel("exec") as ChannelExec
            executor.setCommand("sudo docker exec -it $(sudo docker ps |grep $ambariContainer |cut -d\" \" -f1) file $file")
            executor.setPty(true)
            executor.connect(10000)

            val reader = BufferedReader(InputStreamReader(executor.inputStream))
            var line: String
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(file + ": empty")) {
                    resp = true
                }
            }
        } catch (e: JSchException) {
            Assert.fail(e.message)
        } catch (e: IOException) {
            Assert.fail(e.message)
        } finally {
            if (executor != null) {
                executor.disconnect()
            }
            if (session != null) {
                session.disconnect()
            }
        }
        return resp
    }
}
