#Recipes

With the help of Cloudbreak it is very easy to provision Hadoop clusters in the cloud from an Apache Ambari blueprint. Cloudbreak built in provisioning doesn't contain every use case, so we are introducing the concept of recipes.

Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation. With recipes it's quite easy for example to put a JAR file on the Hadoop classpath or run some custom scripts.

In Cloudbreak we supports two ways to configure recipe, we have downloadable and stored recipes.

##Stored recipes

As the name mentions stored recipes are uploaded and stored in Cloudbreak via web interface or shell.

The easiest way to create a custom recipe:

  * create your own pre and/or post scripts
  * upload them on shell or web interface

###Add recipe

On the web interface under "manage recipes" section you should create new recipe. Please select SCRIPT or FILE type plugin, and fill other required fields.

To add recipe via shell use the following command:

```
recipe store --name [recipe-name] --executionType [ONE_NODE|ALL_NODES] --preInstallScriptFile /path/of/the/pre-install-script --postInstallScriptFile /path/of/the/post-install-script
```

This command has two optional parameters:

- --description "string": description of the recipe
- --timeout "integer": timeout of the script execution
- --publicInAccount "flag": flags if the template is public in the account

In the background Cloudbreak pushes recipe to Consul key/value store during cluster creation.

Stored recipes has limitation on size, because they are stored in Consul key/value store, the base64 encoded content of the scripts must be less then 512kB.

##Downloadable recipes

A downloadable recipe should be available on HTTP, HTTPS protocols optionally with basic authentication, or any kind of public Git repository.

This kind of recipe must contain a plugin.toml file, with some basic information about the recipe, and at least a recipe-pre-install or recipe-post-install script.
Content of plugin.toml:

```
[plugin]
name = "[recipe-name]"
description = "[description-of-the-recipe]"
version = "1.0"
maintainer_name = "[maintainer-name]"
maintainer_email = "[maintainer-email]"
website_url = "[website-url]"
```

Pre- and post scripts are regular shell scripts, and must be executable.

To configure recipe or recipe groups in Cloudbreak you have to create a descriptive JSON file and send it to Cloudbreak via our shell. On web interface you don't need to take care of this file.
```
{
  "name": "[recipe-name]",
  "description": "[description-of-the-recipe]",
  "properties": {
    "[key]": "[value]"
  },
  "plugins": {
      "git://github.com/account/recipe.git": "ONE_NODE"
      "http://user:password@mydomain.com/my-recipe.tar": "ALL_NODES"
      "https://mydomain.com/my-recipe.zip": "ALL_NODES"
  }
}
```

At this point we need to understand some element of the JSON above.

First of all properties. Properties are saved to Consul key/value store, and they are available from the pre or post script by fetching http://localhost:8500/v1/kv/[key]?raw url. The limitation of the value's base64 representation is 512kB. This option is a good choice if you want to write reusable recipes.

The next one is plugins. As you read before we support a few kind of protocols, and each of them has their own limitations:

  * Git
    * git repository must be public (or available from the cluster)
    * the recipe files must be on the root
    * only repository default branch supported, there is no opportunity to check out different branch

  * HTTP(S)
    * on this kind of protocols you have to bundle your recipe into a tar or zip file
    * basic authentication is the only way to protect recipe from public

Last one is the execution type of the recipe. We supports two options:

  * ONE_NODE means the recipe will execute only one node in the hostgroup
  * All_NODES runs every single instance in the hostgroup.

###Add recipe

On the web interface please select URL type plugin, and fill other required fields.

To add recipe via shell use the command(s) below:

```
recipe add --file /path/of/the/recipe/json
```
or
```
recipe add --url http(s)://mydomain.com/my-recipe.json
```

Add command has an optional parameter

- --publicInAccount, flags if the template is public in the account.
