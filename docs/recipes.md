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

## Sample recipe for Ranger

To be able to install Ranger from a blueprint, a database must be running when Ambari starts to install Ranger Admin. With Cloudbreak a database can be configured and started from a recipe. We've created a sample recipe that can be used to initialize and start a PostgreSQL database that will be able to accept connections from Ranger and store its data. Add the `ONE_NODE` recipe from [this URL](https://github.com/sequenceiq/consul-plugins-ranger-db.git) on the Cloudbreak UI:

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/ranger-recipe.png)

And add this recipe to the same hostgroup where Ranger Admin is installed under 'Show Advanced Options' when creating a new cluster:

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/ranger-hostgroup.png)

Ranger installation also has some required properties that must be added to the blueprint. We've created a sample one-node blueprint with the necessary configurations to install Ranger Admin and Ranger Usersync. The configuration values in this blueprint match the sample recipe above - they are set to use a PostgreSQL database on the same host where Ranger Admin is installed. Usersync is configured to use UNIX as the authentication method and it should also be installed on the same host where Ranger Admin is installed.

```
{
  "configurations": [
    {
      "ranger-site": {
        "properties_attributes": {},
        "properties": {}
      }
    },
    {
      "ranger-hdfs-policymgr-ssl": {
        "properties_attributes": {},
        "properties": {
          "xasecure.policymgr.clientssl.keystore": "/etc/hadoop/conf/ranger-plugin-keystore.jks",
          "xasecure.policymgr.clientssl.keystore.credential.file": "jceks://file{{credential_file}}",
          "xasecure.policymgr.clientssl.truststore": "/etc/hadoop/conf/ranger-plugin-truststore.jks",
          "xasecure.policymgr.clientssl.truststore.credential.file": "jceks://file{{credential_file}}"
        }
      }
    },
    {
      "ranger-ugsync-site": {
        "properties_attributes": {},
        "properties": {
          "ranger.usersync.enabled": "true",
          "ranger.usersync.filesource.file": "/tmp/usergroup.txt",
          "ranger.usersync.filesource.text.delimiter": ",",
          "ranger.usersync.group.memberattributename": "member",
          "ranger.usersync.group.nameattribute": "cn",
          "ranger.usersync.group.objectclass": "groupofnames",
          "ranger.usersync.group.searchbase": "ou=groups,dc=hadoop,dc=apache,dc=org",
          "ranger.usersync.group.searchenabled": "false",
          "ranger.usersync.group.searchfilter": "empty",
          "ranger.usersync.group.searchscope": "sub",
          "ranger.usersync.group.usermapsyncenabled": "false",
          "ranger.usersync.ldap.bindalias": "testldapalias",
          "ranger.usersync.ldap.binddn": "cn=admin,dc=xasecure,dc=net",
          "ranger.usersync.ldap.bindkeystore": "-",
          "ranger.usersync.ldap.groupname.caseconversion": "lower",
          "ranger.usersync.ldap.searchBase": "dc=hadoop,dc=apache,dc=org",
          "ranger.usersync.ldap.url": "ldap://localhost:389",
          "ranger.usersync.ldap.user.groupnameattribute": "memberof, ismemberof",
          "ranger.usersync.ldap.user.nameattribute": "cn",
          "ranger.usersync.ldap.user.objectclass": "person",
          "ranger.usersync.ldap.user.searchbase": "ou=users,dc=xasecure,dc=net",
          "ranger.usersync.ldap.user.searchfilter": "empty",
          "ranger.usersync.ldap.user.searchscope": "sub",
          "ranger.usersync.ldap.username.caseconversion": "lower",
          "ranger.usersync.logdir": "/var/log/ranger/usersync",
          "ranger.usersync.pagedresultsenabled": "true",
          "ranger.usersync.pagedresultssize": "500",
          "ranger.usersync.policymanager.baseURL": "{{ranger_external_url}}",
          "ranger.usersync.policymanager.maxrecordsperapicall": "1000",
          "ranger.usersync.policymanager.mockrun": "false",
          "ranger.usersync.port": "5151",
          "ranger.usersync.sink.impl.class": "org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder",
          "ranger.usersync.sleeptimeinmillisbetweensynccycle": "5",
          "ranger.usersync.source.impl.class": "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder",
          "ranger.usersync.ssl": "true",
          "ranger.usersync.unix.minUserId": "500"
        }
      }
    },
    {
      "admin-properties": {
        "properties_attributes": {},
        "properties": {
          "DB_FLAVOR": "POSTGRES",
          "SQL_COMMAND_INVOKER": "psql",
          "SQL_CONNECTOR_JAR": "/var/lib/ambari-agent/tmp/postgres-jdbc-driver.jar",
          "audit_db_name": "ranger_audit",
          "audit_db_user": "rangerlogger",
          "db_host": "localhost:5432",
          "db_name": "ranger",
          "db_root_user": "postgres",
          "db_root_password": "admin",
          "db_user": "rangeradmin",
          "policymgr_external_url": "http://localhost:6080",
          "ranger_jdbc_connection_url": "jdbc:postgresql://{db_host}/ranger",
          "ranger_jdbc_driver": "org.postgresql.Driver"
        }
      }
    },
    {
      "ranger-admin-site": {
        "properties_attributes": {},
        "properties": {
          "ranger.audit.source.type": "db",
          "ranger.authentication.method": "UNIX",
          "ranger.credential.provider.path": "/etc/ranger/admin/rangeradmin.jceks",
          "ranger.externalurl": "{{ranger_external_url}}",
          "ranger.https.attrib.keystore.file": "/etc/ranger/admin/keys/server.jks",
          "ranger.jpa.audit.jdbc.credential.alias": "rangeraudit",
          "ranger.jpa.audit.jdbc.dialect": "{{jdbc_dialect}}",
          "ranger.jpa.audit.jdbc.driver": "{{jdbc_driver}}",
          "ranger.jpa.audit.jdbc.url": "{{audit_jdbc_url}}",
          "ranger.jpa.audit.jdbc.user": "{{ranger_audit_db_user}}",
          "ranger.jpa.jdbc.credential.alias": "rangeradmin",
          "ranger.jpa.jdbc.dialect": "{{jdbc_dialect}}",
          "ranger.jpa.jdbc.driver": "org.postgresql.Driver",
          "ranger.jpa.jdbc.url": "jdbc:postgresql://localhost:5432/ranger",
          "ranger.jpa.jdbc.user": "{{ranger_db_user}}",
          "ranger.jpa.jdbc.password": "{{ranger_db_password}}",
          "ranger.ldap.ad.domain": "localhost",
          "ranger.ldap.ad.url": "ldap://ad.xasecure.net:389",
          "ranger.ldap.group.roleattribute": "cn",
          "ranger.ldap.group.searchbase": "ou=groups,dc=xasecure,dc=net",
          "ranger.ldap.group.searchfilter": "(member=uid={0},ou=users,dc=xasecure,dc=net)",
          "ranger.ldap.url": "ldap://localhost:389",
          "ranger.ldap.user.dnpattern": "uid={0},ou=users,dc=xasecure,dc=net",
          "ranger.service.host": "{{ranger_host}}",
          "ranger.service.http.enabled": "true",
          "ranger.service.http.port": "6080",
          "ranger.service.https.attrib.clientAuth": "false",
          "ranger.service.https.attrib.keystore.keyalias": "mkey",
          "ranger.service.https.attrib.keystore.pass": "ranger",
          "ranger.service.https.attrib.ssl.enabled": "false",
          "ranger.service.https.port": "6182",
          "ranger.solr.url": "http://solr_host:6083/solr/ranger_audits",
          "ranger.unixauth.remote.login.enabled": "true",
          "ranger.unixauth.service.hostname": "localhost",
          "ranger.unixauth.service.port": "5151"
        }
      }
    },
    {
      "ranger-env": {
        "properties_attributes": {},
        "properties": {
          "admin_username": "admin",
          "create_db_dbuser": "true",
          "ranger_admin_log_dir": "/var/log/ranger/admin",
          "ranger_admin_username": "amb_ranger_admin",
          "ranger_admin_password": "amb_ranger_pw",
          "ranger_group": "ranger",
          "ranger_jdbc_connection_url": "{{ranger_jdbc_connection_url}}",
          "ranger_jdbc_driver": "org.postgresql.Driver",
          "ranger_pid_dir": "/var/run/ranger",
          "ranger_user": "ranger",
          "ranger_usersync_log_dir": "/var/log/ranger/usersync",
          "xml_configurations_supported": "true"
        }
      }
    },
    {
      "ranger-yarn-security": {
        "properties_attributes": {},
        "properties": {
          "ranger.plugin.yarn.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache",
          "ranger.plugin.yarn.policy.pollIntervalMs": "30000",
          "ranger.plugin.yarn.policy.rest.ssl.config.file": "/etc/yarn/conf/ranger-policymgr-ssl.xml",
          "ranger.plugin.yarn.policy.rest.url": "{{policymgr_mgr_url}}",
          "ranger.plugin.yarn.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient",
          "ranger.plugin.yarn.service.name": "{{repo_name}}"
        }
      }
    },
    {
      "ranger-yarn-audit": {
        "properties_attributes": {},
        "properties": {
          "xasecure.audit.credential.provider.file": "jceks://file{{credential_file}}",
          "xasecure.audit.db.async.max.flush.interval.ms": "30000",
          "xasecure.audit.db.async.max.queue.size": "10240",
          "xasecure.audit.db.batch.size": "100",
          "xasecure.audit.db.is.async": "true",
          "xasecure.audit.db.is.enabled": "false",
          "xasecure.audit.hdfs.async.max.flush.interval.ms": "30000",
          "xasecure.audit.hdfs.async.max.queue.size": "1048576",
          "xasecure.audit.hdfs.config.destination.directory": "hdfs://NAMENODE_HOST:8020/ranger/audit/%app-type%/%time:yyyyMMdd%",
          "xasecure.audit.hdfs.config.destination.file": "%hostname%-audit.log",
          "xasecure.audit.hdfs.config.destination.flush.interval.seconds": "900",
          "xasecure.audit.hdfs.config.destination.open.retry.interval.seconds": "60",
          "xasecure.audit.hdfs.config.destination.rollover.interval.seconds": "86400",
          "xasecure.audit.hdfs.config.encoding": "",
          "xasecure.audit.hdfs.config.local.archive.directory": "/var/log/yarn/audit/archive",
          "xasecure.audit.hdfs.config.local.archive.max.file.count": "10",
          "xasecure.audit.hdfs.config.local.buffer.directory": "/var/log/yarn/audit",
          "xasecure.audit.hdfs.config.local.buffer.file": "%time:yyyyMMdd-HHmm.ss%.log",
          "xasecure.audit.hdfs.config.local.buffer.file.buffer.size.bytes": "8192",
          "xasecure.audit.hdfs.config.local.buffer.flush.interval.seconds": "60",
          "xasecure.audit.hdfs.config.local.buffer.rollover.interval.seconds": "600",
          "xasecure.audit.hdfs.is.async": "true",
          "xasecure.audit.hdfs.is.enabled": "false",
          "xasecure.audit.is.enabled": "true",
          "xasecure.audit.jpa.javax.persistence.jdbc.driver": "{{jdbc_driver}}",
          "xasecure.audit.jpa.javax.persistence.jdbc.url": "{{audit_jdbc_url}}",
          "xasecure.audit.jpa.javax.persistence.jdbc.user": "{{xa_audit_db_user}}",
          "xasecure.audit.kafka.async.max.flush.interval.ms": "1000",
          "xasecure.audit.kafka.async.max.queue.size": "1",
          "xasecure.audit.kafka.broker_list": "localhost:9092",
          "xasecure.audit.kafka.is.enabled": "false",
          "xasecure.audit.kafka.topic_name": "ranger_audits",
          "xasecure.audit.log4j.async.max.flush.interval.ms": "30000",
          "xasecure.audit.log4j.async.max.queue.size": "10240",
          "xasecure.audit.log4j.is.async": "false",
          "xasecure.audit.log4j.is.enabled": "false",
          "xasecure.audit.solr.async.max.flush.interval.ms": "1000",
          "xasecure.audit.solr.async.max.queue.size": "1",
          "xasecure.audit.solr.is.enabled": "false",
          "xasecure.audit.solr.solr_url": "http://localhost:6083/solr/ranger_audits"
        }
      }
    },
    {
      "ranger-hdfs-security": {
        "properties_attributes": {},
        "properties": {
          "ranger.plugin.hdfs.policy.cache.dir": "/etc/ranger/{{repo_name}}/policycache",
          "ranger.plugin.hdfs.policy.pollIntervalMs": "30000",
          "ranger.plugin.hdfs.policy.rest.ssl.config.file": "/etc/hadoop/conf/ranger-policymgr-ssl.xml",
          "ranger.plugin.hdfs.policy.rest.url": "{{policymgr_mgr_url}}",
          "ranger.plugin.hdfs.policy.source.impl": "org.apache.ranger.admin.client.RangerAdminRESTClient",
          "ranger.plugin.hdfs.service.name": "{{repo_name}}",
          "xasecure.add-hadoop-authorization": "true"
        }
      }
    },
    {
      "ranger-yarn-plugin-properties": {
        "properties_attributes": {},
        "properties": {
          "REPOSITORY_CONFIG_USERNAME": "yarn",
          "common.name.for.certificate": "-",
          "hadoop.rpc.protection": "-",
          "policy_user": "ambari-qa",
          "ranger-yarn-plugin-enabled": "No"
        }
      }
    },
    {
      "ranger-hdfs-audit": {
        "properties_attributes": {},
        "properties": {
          "xasecure.audit.credential.provider.file": "jceks://file{{credential_file}}",
          "xasecure.audit.db.async.max.flush.interval.ms": "30000",
          "xasecure.audit.db.async.max.queue.size": "10240",
          "xasecure.audit.db.batch.size": "100",
          "xasecure.audit.db.is.async": "true",
          "xasecure.audit.db.is.enabled": "false",
          "xasecure.audit.hdfs.async.max.flush.interval.ms": "30000",
          "xasecure.audit.hdfs.async.max.queue.size": "1048576",
          "xasecure.audit.hdfs.config.destination.directory": "hdfs://NAMENODE_HOST:8020/ranger/audit/%app-type%/%time:yyyyMMdd%",
          "xasecure.audit.hdfs.config.destination.file": "%hostname%-audit.log",
          "xasecure.audit.hdfs.config.destination.flush.interval.seconds": "900",
          "xasecure.audit.hdfs.config.destination.open.retry.interval.seconds": "60",
          "xasecure.audit.hdfs.config.destination.rollover.interval.seconds": "86400",
          "xasecure.audit.hdfs.config.encoding": "",
          "xasecure.audit.hdfs.config.local.arcïœ©ïœ©ïœ©ïœ©ïœ©.directory": "/var/log/hadoop/audit/archive/%app-type%",
          "xasecure.audit.hdfs.config.local.archive.max.file.count": "10",
          "xasecure.audit.hdfs.config.local.buffer.directory": "/var/log/hadoop/audit/%app-type%",
          "xasecure.audit.hdfs.config.local.buffer.file": "%time:yyyyMMdd-HHmm.ss%.log",
          "xasecure.audit.hdfs.config.local.buffer.file.buffer.size.bytes": "8192",
          "xasecure.audit.hdfs.config.local.buffer.flush.interval.seconds": "60",
          "xasecure.audit.hdfs.config.local.buffer.rollover.interval.seconds": "600",
          "xasecure.audit.hdfs.is.async": "true",
          "xasecure.audit.hdfs.is.enabled": "false",
          "xasecure.audit.is.enabled": "true",
          "xasecure.audit.jpa.javax.persistence.jdbc.driver": "{{jdbc_driver}}",
          "xasecure.audit.jpa.javax.persistence.jdbc.url": "{{audit_jdbc_url}}",
          "xasecure.audit.jpa.javax.persistence.jdbc.user": "{{xa_audit_db_user}}",
          "xasecure.audit.kafka.async.max.flush.interval.ms": "1000",
          "xasecure.audit.kafka.async.max.queue.size": "1",
          "xasecure.audit.kafka.broker_list": "localhost:9092",
          "xasecure.audit.kafka.is.enabled": "false",
          "xasecure.audit.kafka.topic_name": "ranger_audits",
          "xasecure.audit.log4j.async.max.flush.interval.ms": "30000",
          "xasecure.audit.log4j.async.max.queue.size": "10240",
          "xasecure.audit.log4j.is.async": "false",
          "xasecure.audit.log4j.is.enabled": "false",
          "xasecure.audit.solr.async.max.flush.interval.ms": "1000",
          "xasecure.audit.solr.async.max.queue.size": "1",
          "xasecure.audit.solr.is.enabled": "false",
          "xasecure.audit.solr.solr_url": "http://localhost:6083/solr/ranger_audits"
        }
      }
    },
    {
      "ranger-hdfs-plugin-properties": {
        "properties_attributes": {},
        "properties": {
          "REPOSITORY_CONFIG_USERNAME": "hadoop",
          "common.name.for.certificate": "-",
          "hadoop.rpc.protection": "-",
          "policy_user": "ambari-qa",
          "ranger-hdfs-plugin-enabled": "No"
        }
      }
    },
    {
      "usersync-properties": {
        "properties_attributes": {},
        "properties": {}
      }
    }
  ],
  "host_groups": [
    {
      "components": [
        {
          "name": "NODEMANAGER"
        },
        {
          "name": "YARN_CLIENT"
        },
        {
          "name": "HDFS_CLIENT"
        },
        {
          "name": "HISTORYSERVER"
        },
        {
          "name": "METRICS_MONITOR"
        },
        {
          "name": "NAMENODE"
        },
        {
          "name": "ZOOKEEPER_CLIENT"
        },
        {
          "name": "RANGER_ADMIN"
        },
        {
          "name": "SECONDARY_NAMENODE"
        },
        {
          "name": "MAPREDUCE2_CLIENT"
        },
        {
          "name": "ZOOKEEPER_SERVER"
        },
        {
          "name": "AMBARI_SERVER"
        },
        {
          "name": "DATANODE"
        },
        {
          "name": "RANGER_USERSYNC"
        },
        {
          "name": "APP_TIMELINE_SERVER"
        },
        {
          "name": "METRICS_COLLECTOR"
        },
        {
          "name": "RESOURCEMANAGER"
        }
      ],
      "configurations": [],
      "name": "host_group_1",
      "cardinality": "1"
    }
  ],
  "Blueprints": {
    "stack_name": "HDP",
    "stack_version": "2.3",
    "blueprint_name": "ranger-psql-onenode-sample"
  }
}
```

*Notes*

- Ranger plugins cannot be enabled by default in a blueprint due to some Ambari restrictions, so properties like `ranger-hdfs-plugin-enabled` must be set to *No* and the plugins must be enabled from the Ambari UI with the checkboxes and by restarting the necessary services.
- If using the UNIX user sync, it may be necessary in some cases to restart the Ranger Usersync Services after the blueprint installation finished if the UNIX users cannot be seen on the Ranger Admin UI.
