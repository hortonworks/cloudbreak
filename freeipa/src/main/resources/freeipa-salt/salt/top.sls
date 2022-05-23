base:
           '*':
             - freeipa.grow-disk
             - nginx
             - tags
             - recipes.runner
             - telemetry
             - fluent
             - ntp
             - freeipa
             - freeipa.services
             - dns
             - logrotate
             - ccm
             - monitoring

           'recipes:pre-cloudera-manager-start':
             - match: grain
             - recipes.pre-cloudera-manager-start

           'roles:freeipa_primary':
             - match: grain
             - freeipa.primary-install
             - freeipa.common-install
             - freeipa.backups
             - freeipa.healthagent
             - nodestatus
             - freeipa.patch-pki-tomcat

           'roles:freeipa_replica':
             - match: grain
             - freeipa.replica-install
             - freeipa.common-install
             - freeipa.backups
             - freeipa.healthagent
             - nodestatus
             - freeipa.patch-pki-tomcat

           'roles:freeipa_primary_replacement':
             - match: grain
             - freeipa.replica-install
             - freeipa.common-install
             - freeipa.promote-replica-to-master
             - freeipa.backups
             - freeipa.healthagent
             - nodestatus
             - freeipa.patch-pki-tomcat

           'recipes:post-cloudera-manager-start':
             - match: grain
             - recipes.post-cloudera-manager-start

           'recipes:post-cluster-install':
             - match: grain
             - recipes.post-cluster-install

           'recipes:pre-termination':
             - match: grain
             - recipes.pre-termination