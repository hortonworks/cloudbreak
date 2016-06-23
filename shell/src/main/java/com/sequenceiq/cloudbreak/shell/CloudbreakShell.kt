package com.sequenceiq.cloudbreak.shell

import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.Optional

import javax.inject.Inject

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.Banner
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.shell.CommandLine
import org.springframework.shell.core.CommandResult
import org.springframework.shell.core.JLineShellComponent
import org.springframework.shell.event.ShellStatus
import org.springframework.shell.event.ShellStatusListener

import com.sequenceiq.cloudbreak.api.model.NetworkJson
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson
import com.sequenceiq.cloudbreak.api.model.VmTypeJson
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.model.Hints
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.sequenceiq.cloudbreak.shell")
class CloudbreakShell : CommandLineRunner, ShellStatusListener {

    @Inject
    private val commandLine: CommandLine? = null
    @Inject
    private val shell: JLineShellComponent? = null
    @Inject
    private val context: ShellContext? = null
    @Inject
    private val cloudbreakClient: CloudbreakClient? = null
    @Inject
    private val responseTransformer: ResponseTransformer<Collection<Any>>? = null

    @Value("${sequenceiq.user:}")
    private val user: String? = null

    @Value("${sequenceiq.password:}")
    private val password: String? = null

    @Throws(Exception::class)
    override fun run(vararg arg: String) {
        if ("" == user) {
            println("Missing 'sequenceiq.user' parameter!")
            return
        }
        if ("" == password) {
            println("Missing 'sequenceiq.password' parameter!")
            return
        }
        val shellCommandsToExecute = commandLine!!.shellCommandsToExecute
        if (shellCommandsToExecute != null) {
            init()
            for (cmd in shellCommandsToExecute) {
                val replacedCommand = getReplacedString(cmd)
                val commandResult = shell!!.executeCommand(replacedCommand)
                if (!commandResult.isSuccess) {
                    val message = Optional.ofNullable(commandResult.exception).map<String>(Function<Throwable, String> { it.message }).orElse("Unknown error, maybe command not valid.")
                    println(String.format("%s: [%s] [REASON: %s]", replacedCommand, FAILED, message))
                    break
                } else {
                    println(String.format("%s: [%s]", replacedCommand, SUCCESS))
                }
            }
        } else {
            shell!!.addShellStatusListener(this)
            shell.start()
            shell.promptLoop()
            shell.waitForComplete()
        }
    }

    private fun getReplacedString(cmd: String): String {
        var result: String? = cmd
        if (result != null) {
            for (split in cmd.split(SPACE.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                if (split.startsWith(DOLLAR)) {
                    result = result!!.replace(split, System.getenv(split.replace(DOLLAR, EMPTY)))
                }
            }
        }
        return result
    }

    override fun onShellStatusChange(oldStatus: ShellStatus, newStatus: ShellStatus) {
        if (newStatus.status == ShellStatus.Status.STARTED) {
            try {
                init()
            } catch (e: Exception) {
                println("Can't connect to Cloudbreak")
                e.printStackTrace()
                shell!!.executeCommand("quit")
            }

        }
    }

    @Throws(Exception::class)
    private fun init() {
        //cloudbreak.health();
        initResourceAccessibility()
        initPlatformVariants()
        if (!context!!.isCredentialAccessible) {
            context.setHint(Hints.CREATE_CREDENTIAL)
        } else {
            context.setHint(Hints.SELECT_CREDENTIAL)
        }
    }

    @Throws(Exception::class)
    private fun initResourceAccessibility() {
        if (!cloudbreakClient!!.credentialEndpoint().publics.isEmpty()) {
            context!!.setCredentialAccessible()
        }
        if (!cloudbreakClient.blueprintEndpoint().publics.isEmpty()) {
            context!!.setBlueprintAccessible()
        }
        if (!cloudbreakClient.stackEndpoint().publics.isEmpty()) {
            context!!.setStackAccessible()
        }
        if (!cloudbreakClient.recipeEndpoint().publics.isEmpty()) {
            context!!.setRecipeAccessible()
        }
        if (!cloudbreakClient.sssdConfigEndpoint().publics.isEmpty()) {
            context!!.setSssdConfigAccessible()
        }
        val publics = cloudbreakClient.networkEndpoint().publics
        for (network in publics) {
            context!!.putNetwork(java.lang.Long.valueOf(network.id), network.cloudPlatform)
        }
        val securityGroups = cloudbreakClient.securityGroupEndpoint().publics
        for (securityGroup in securityGroups) {
            context!!.putSecurityGroup(securityGroup.id, securityGroup.name)
        }
    }

    private fun initPlatformVariants() {
        var platformToVariants: Map<String, Collection<String>> = Collections.EMPTY_MAP
        var regions: Map<String, Collection<String>> = Collections.EMPTY_MAP
        var volumeTypes: Map<String, Collection<String>> = Collections.EMPTY_MAP
        var availabilityZones: Map<String, Map<String, Collection<String>>> = Collections.EMPTY_MAP
        val instanceTypes = HashMap<String, List<Map<String, String>>>()
        var orchestrators: Map<String, Collection<String>> = HashMap()
        try {
            platformToVariants = cloudbreakClient!!.connectorEndpoint().platformVariants.platformToVariants
            regions = cloudbreakClient.connectorEndpoint().regions.regions
            availabilityZones = cloudbreakClient.connectorEndpoint().regions.availabilityZones
            volumeTypes = cloudbreakClient.connectorEndpoint().disktypes.diskTypes
            orchestrators = cloudbreakClient.connectorEndpoint().orchestratortypes.orchestrators
            val virtualMachines = cloudbreakClient.connectorEndpoint().vmTypes.virtualMachines
            for (vmCloud in virtualMachines.entries) {
                val tmp = ArrayList<Map<String, String>>()
                for (vmTypeJson in vmCloud.value) {
                    val map = responseTransformer!!.transformObjectToStringMap(vmTypeJson)
                    tmp.add(map)
                }
                instanceTypes.put(vmCloud.key, tmp)
            }
        } catch (e: Exception) {
            println("Error during retrieving platform variants")
        } finally {
            context!!.setPlatformToVariantsMap(platformToVariants)
            context.setRegions(regions)
            context.setAvailabilityZones(availabilityZones)
            context.setVolumeTypes(volumeTypes)
            context.setInstanceTypes(instanceTypes)
            context.setOrchestrators(orchestrators)
        }
    }

    companion object {

        val DOLLAR = "$"
        val SPACE = " "
        val EMPTY = ""
        val FAILED = "FAILED"
        val SUCCESS = "SUCCESS"

        @Throws(IOException::class)
        @JvmStatic fun main(args: Array<String>) {

            if (args.size == 1 && ("--help" == args[0] || "-h" == args[0]) || args.size == 0) {
                println("\nCloudbreak Shell: Interactive command line tool for managing Cloudbreak.\n\n"
                        + "Usage:\n"
                        + "  java -jar cloudbreak-shell.jar                  : Starts Cloudbreak Shell in interactive mode.\n"
                        + "  java -jar cloudbreak-shell.jar --cmdfile=<FILE> : Cloudbreak Shell executes commands read from the file.\n\n"
                        + "Options:\n"
                        + "  --cloudbreak.address=http[s]://<HOSTNAME>[:PORT]  Address of the Cloudbreak Server\n"
                        + "  --identity.address=http[s]://<HOSTNAME>[:PORT]    Address of the SequenceIQ identity server (not a mandatory parameter)"
                        + " [default: cloudbreak.address + /identity].\n"
                        + "  --sequenceiq.user=<USER>                          Username of the SequenceIQ user.\n"
                        + "  --sequenceiq.password=<PASSWORD>                  Password of the SequenceIQ user.\n"
                        + "  --cert.validation=<boolean>                       Validate SSL certificates, shall be disabled for self signed certificates"
                        + " (not a mandatory parameter) [default: true].")
                return
            } else {
                if (!VersionedApplication.versionedApplication().showVersionInfo(args)) {
                    try {
                        SpringApplicationBuilder(CloudbreakShell::class.java).web(false).bannerMode(Banner.Mode.OFF).run(*args)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        println("Cloudbreak shell cannot be started.")
                    }

                }
            }
        }
    }

}
