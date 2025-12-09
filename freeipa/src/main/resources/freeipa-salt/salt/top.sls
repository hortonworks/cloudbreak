base:
           '*':
             - prevalidation
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
             - rhelrepo
             - hostname
             - java
             - loadbalancer.loadbalancer_ip

           'recipes:pre-cloudera-manager-start':
             - match: grain
             - recipes.pre-cloudera-manager-start

           'recipes:pre-service-deployment':
             - match: grain
             - recipes.pre-service-deployment

           'roles:freeipa_primary':
             - match: grain
             - freeipa.primary-install
             - freeipa.common-install
             {% if salt['pillar.get']('environmentType', 'PUBLIC_CLOUD') == 'HYBRID' %}
             - trustsetup.adtrust_install
             {% endif %}
             - freeipa.backups
             {% if salt['pillar.get']('freeipa:secretEncryptionEnabled', False) == True %}
             - cdpluksvolumebackup
             {% endif %}
             - freeipa.healthagent
             - freeipa.iptables
             - freeipa.patch-pki-tomcat
             - freeipa.ldapagent
             - freeipa.selinux-mode
             - faillock
             - saltsyncer

           'roles:freeipa_replica':
             - match: grain
             - freeipa.replica-install
             - freeipa.common-install
             {% if salt[ 'pillar.get' ]('environmentType', 'PUBLIC_CLOUD') == 'HYBRID' %}
             - trustsetup.adtrust_install
             {% endif %}
             - freeipa.backups
             {% if salt['pillar.get']('freeipa:secretEncryptionEnabled', False) == True %}
             - cdpluksvolumebackup
             {% endif %}
             - freeipa.healthagent
             - freeipa.patch-pki-tomcat
             - freeipa.ldapagent
             - freeipa.selinux-mode
             - faillock

           'roles:freeipa_primary_replacement':
             - match: grain
             - freeipa.replica-install
             - freeipa.common-install
             - freeipa.promote-replica-to-master
             {% if salt[ 'pillar.get' ]('environmentType', 'PUBLIC_CLOUD') == 'HYBRID' %}
             - trustsetup.adtrust_install
             {% endif %}
             - freeipa.backups
             {% if salt['pillar.get']('freeipa:secretEncryptionEnabled', False) == True %}
             - cdpluksvolumebackup
             {% endif %}
             - freeipa.healthagent
             - freeipa.patch-pki-tomcat
             - freeipa.ldapagent
             - freeipa.selinux-mode
             - faillock

           'recipes:post-cluster-install':
             - match: grain
             - recipes.post-cluster-install

           'recipes:post-service-deployment':
             - match: grain
             - recipes.post-service-deployment

           'recipes:pre-termination':
             - match: grain
             - recipes.pre-termination