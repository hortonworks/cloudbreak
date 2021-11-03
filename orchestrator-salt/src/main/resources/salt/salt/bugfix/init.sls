restart_unbound_fix:
  file.replace:
    - name: "/etc/dhcp/dhclient-enter-hooks"
    - pattern: "systemctl restart unbound"
    - repl: "pkill -u unbound -SIGHUP unbound"
