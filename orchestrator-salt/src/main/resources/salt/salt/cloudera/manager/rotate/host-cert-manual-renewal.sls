rotate-host-cert:
  cmd.run:
    - name: /opt/cloudera/cm-agent/service/certs/rotate_host_cert.sh /opt/cloudera/cm-agent/bin/cm "$(cat /etc/cloudera-scm-agent/cmagent.token)"