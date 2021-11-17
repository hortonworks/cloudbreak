# PostgreSQL Disaster Recovery Scripts
The `disaster_recovery` directory holds Salt states and Bash scripts for backing up and restoring PostgreSQL servers attached to CloudBreak managed infrastructure.

This collection of scripts and salt state originally only handled backing up PostgreSQL instances attached to DL master nodes, and handling Ranger and Hive Metastore (HMS) data.
It has since been extended to handle backing up databases attached to Datahubs.

# Salt
Salt is a distributed coordination and management system that CloudBreak uses to run scripts, copy files, and otherwise administrate VMs that it manages.

You may find the [Introduction to Salt](https://docs.saltproject.io/en/latest/topics/index.html) useful to get started with Salt.

Disaster Recovery consists of two main parts:
* Backup
* Restore

We define `.sls` (Salt State Formula) files as the entry point to run backup and restore scripts through Salt.

`init.sls` acts as the entry point to the `disaster_recovery` directory. The name is a Salt convention and allows us to refer to the directory as part of Salt commands.

From a high level, both `backup.sls` and `recover.sls` define Salt states that run either the `backup_db.sh` or `restore_db.sh` scripts.
These script states _require_ states defined in `init.sls`:
* copy the backup script to the CB managed node
* copy the restore script to the CB managed node
* copy the `.pgpass` file to the CB managed node
* set the `PGPASSFILE` environment variable on the CB managed node

Salt mostly uses YAML for configuration values.

It also provides templating support in a number of languages, we prefer Jinja templating in CloudBreak. If you see odd things in Yaml, `.sh`, or the `.pgpass` file, it's probably Jinja.
# PostgreSQL
Since we use PostgreSQL as our database engine of choice, backup and restore of the DB relies heavily on PostgreSQL files, variables, and commands.

The PostgreSQL 12 docs are a good place to get more information, in particular:
* [The `psql` command](https://www.postgresql.org/docs/12/app-psql.html)
* [Backup and restore documentation](https://www.postgresql.org/docs/12/backup.html)
* [The `pg_dump` command](https://www.postgresql.org/docs/12/app-pgdump.html), used to run the backups
* [The `.pgpass` password file](https://www.postgresql.org/docs/12/libpq-pgpass.html)

# Development
The easiest way to test changes to Salt scripts rapidly is to set up an environment using CB, then ssh into a node and run salt commands.

## SSH into the node
You should ssh into the master node of your DL, the IP address can be found on the Hardware tab of the DL page on CDP.

For example:
```shell
$ ssh cloudbreak@10.80.173.222
```

You may have to locate the correct cloudbreak key, and provide it to the SSH command:
```shell
$ ssh -i ~/.ssh/my_key cloudbreak@10.80.173.222
```

## Activate Salt
Once you're sshed into the master node, become root:
```shell
$ sudo su -
```

Then activate salt by `source`ing the `activate script:
```shell
$ source /opt/salt_3000.8/bin/activate
```
The path may be different depending on the version of Salt in use! You should double check `/opt` for the appropriate `salt_XXXX.X` directory.

## Invoke salt commands
Now, you can run salt commands.

Target all minions with a ping command:
```shell
$ salt '*' test.ping
```
You should see all the nodes in the DL cluster. Note that the master node is both a master _and_ a minion in CB!

Now, we can try running a backup command, targeting the node we're running on (the master):
```shell
$ salt $(hostname -f) state.apply postgresql.disaster_recovery.backup
```

### Failed Backup
The above command **will fail on a DL that has never had a backup from the CDP CLI run**.
The failed backup command is demonstrative of a how Salt failures are displayed.
```shell
----------
          ID: backup_postgresql_db
    Function: cmd.run
        Name: /opt/salt/scripts/backup_db.sh None "" "" "" None True
      Result: False
     Comment: Command "/opt/salt/scripts/backup_db.sh None "" "" "" None True" run
     Started: 21:31:16.059148
    Duration: 10.012 ms
     Changes:
              ----------
              pid:
                  21661
              retcode:
                  1
              stderr:
                  + [[ 6 -lt 6 ]]
                  + [[ 6 -gt 7 ]]
                  + [[ None == \N\o\n\e ]]
                  + echo 'Invalid inputs provided'
                  + echo 'Script accepts at least 6 and at most 7 inputs:'
                  + echo '  1. Object Storage Service url to place backups.'
                  + echo '  2. PostgreSQL host name.'
                  + echo '  3. PostgreSQL port.'
                  + echo '  4. PostgreSQL user name.'
                  + echo '  5. Ranger admin group.'
                  + echo '  6. Whether or not to close connections for the database while it is being backed up.'
                  + echo '  7. (optional) Name of the database to backup. If not given, will backup ranger and hive databases.'
                  + exit 1
              stdout:
                  Invalid inputs provided
                  Script accepts at least 6 and at most 7 inputs:
                    1. Object Storage Service url to place backups.
                    2. PostgreSQL host name.
                    3. PostgreSQL port.
                    4. PostgreSQL user name.
                    5. Ranger admin group.
                    6. Whether or not to close connections for the database while it is being backed up.
                    7. (optional) Name of the database to backup. If not given, will backup ranger and hive databases.

Summary for bderriso-arco-sdx-master0.bderriso.xcu2-8y8x.dev.cldr.work
------------
Succeeded: 6 (changed=7)
Failed:    1
------------
Total states run:     7
Total run time: 541.040 ms
ERROR: Minions returned with non-zero exit code
```
We can see that the `/opt/salt/scripts/backup_db.sh` script failed because it did not have the correct arguments.

Arguments to the script are provided by the pillar system, and during a normal backup, CloudBreak will populate the pillar values for us.
We can manually update the pillar values ourselves.

## Manually edit files
The Salt Pillar values are held in `/srv/pillar`, we can change directories there and see that the contents mirror the contents of the `orchestrator-salt/src/main/resources/salt/pillar`
directory.

```shell
$ cd /srv/pillar
$ ls
```

Now, we can edit the right pillar to run a backup command.

The `disaster_recovery.sls` file in `` is essentially a placeholder. It has content, but that content is entirely replaced when CB updates the pillar:
From:
```yml
disaster_recovery:
  object_storage_url:
  ranger_admin_group:
  database_name:
  close_connections: true
```

To:
```json
#!json
{
        "disaster_recovery": {
                "close_connections": "true",
                "object_storage_url": "s3a://eng-sdx-daily-v2-datalake/bderriso-arco/logs/backup/92f76652-1b46-4b49-a0ab-001aaddaca03_database_backup",
                "ranger_admin_group": "_c_ranger_admins_7df4e583"
        }
}
```
You can manually edit the `disaster_recovery.sls` file to match the above JSON, replacing the `object_storage_url` with the logs directory of your DL.

## Copy files from your local to the node
We'll use `rsync` to run a remote copy onto the node.

You should use `ssh-add` to add the ssh key for connecting to the node to your `ssh-agent`. It will greatly reduce the verbosity of `rsync` commands.
From the root of the CloudBreak project directory, run:
```shell
$ rsync -av ./orchestrator-salt/src/main/resources/salt/salt/postgresql/disaster_recovery cloudbreak@{IP_ADDRESS}:~/
```

Now, we ssh into the node, become root, and rsync again:
```shell
$ ssh cloudbreak@${IP_ADDRESS}
$ sudo su -
$ rsync -av /home/cloudbreak/disaster_recovery/ /srv/salt/postgresql/disaster_recovery/
```
