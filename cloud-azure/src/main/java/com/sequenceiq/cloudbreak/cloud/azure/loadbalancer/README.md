# Azure Load Balancers
High level overview of Azure Load Balancers is available in the [Azure public documentation](https://docs.microsoft.com/en-us/azure/load-balancer/load-balancer-overview).

We deploy Azure Load Balancers alongside other CB managed infrastructure by plugging load balancer related values into an Azure Resource Manager (ARM) template.

## Azure Resource Manager and Load Balancer
We have to provide values for most of the Azure Load Balancer components, a useful overview is provided in [Azure Load Balancer Components](https://docs.microsoft.com/en-us/azure/load-balancer/components).

Namely, we care about configuring the:
* Frontend IP address associated with the LB
* The _backend pool_ containing the VMs to load balance to
* _Health probes_ to monitor the health of the VMs in the backend address pool
* _Rules_ which define how to forward traffic from the load balancer to the correct VMs

Azure has documentation for [valid load balancer ARM template values](https://docs.microsoft.com/en-us/azure/templates/microsoft.network/loadbalancers?tabs=bicep).

We provide a `<#list loadbalancers as loadbalancer>` of Azure resources, with the type `"type": "Microsoft.Network/loadBalancers"`. The JSON object contains a number of properties associated
the LB components listed above. 

The majority of the LB related template code is found in the `<#list>` of load balancers, however there is a dependency in the network interfaces, `"type": "Microsoft.Network/networkInterfaces"`,
on the backend address pool defined by the load balancers.

## Freemarker templates
The ARM template is defined as an [Apache Freemarker Template](https://freemarker.apache.org/).

In particular, the [Freemarker Template Manual](https://freemarker.apache.org/docs/index.html) may be useful.
We also use "sequences" extensively, so the [built-in sequences documentation](https://freemarker.apache.org/docs/ref_builtins_sequence.html) is also useful.

## Java Model
We use `AzureTemplateBuilder.java` to populate the Freemarker template.

We provide a number of Java model objects to the `build` method and then push a `Map<String, Object>` into the Freemarker related code.
Azure Load Balancer creation is sufficiently complicated to warrant it's own `AzureLoadBalancerModelBuilder`, which accepts a `CloudStack` and stack name and returns a
map of the load balancers and load balancer mappings. The load balancer mappings associates a CB instance group with the load balancer that sits in front of it.

# Development
To test Azure Load Balancer changes quickly, you should set up your local CB + CBD development environment.
You should create an environment and test by creating and deleting a data lake using the CDP CLI `cdp datalake create-azure-datalake` and `cdp datalake delete-datalake` commands.

It can be useful to grab the JSON `String` created by the `AzureTemplateBuilder`. This value is logged to the console, but it's sometimes easier to place a
breakpoint and then copy the object value from the debugging console.

Good breakpoint locations are:
* The end of `AzureTemplateBuilder.build()`, where we return the generated template
* In the `AzureResourceConnector`, after the `template` value is created retrieved from the `azureTemplateBuilder.build()`.

Incorrect ARM template or Freemarker template values can cause provisioning to fail. It's useful to take the string value of the template and provide it to the Azure CLI.

To test this, you should have set up an Azure Resource Group already, then run the [`az deployment group what-if`](https://docs.microsoft.com/en-us/cli/azure/deployment/group?view=azure-cli-latest#az_deployment_group_what_if)
to deploy the template to the resource group.

Fully setting up a Resource Group, getting the AZ CLI, and connecting to your subscription are outside the scope of this document. However, the resource group
should be set up to receive a Data Lake deployment, so you may follow the SDX setup scripts or CDP public docs to set up the Resource Group.

Once you have retrieved the ARM template from a breakpoint or log statement, you can check if the JSON is valid and if Azure understands it.
```shell
$ pbpaste | jq . # Assuming you have `jq` installed, and the template held in your Mac clipboard. This will fail if your string is not actually valid JSON.
```

Save the string to a file and run the Azure CLI against it, where `$MY_RESOURCE_GROUP` is the resource group you want to deploy to:
```shell
$ pbpaste > /tmp/arm-v2.json # Assuming the string is in your Mac clipboard
$ az deployment group what-if --resource-group $MY_RESOURCE_GROUP --template-file /tmp/arm-v2.json
```

Then, you can attempt to actually run the deployment:
```shell
$ az deployment group create --resource-group $MY_RESOURCE_GROUP --template-file /tmp/arm-v2.json
```