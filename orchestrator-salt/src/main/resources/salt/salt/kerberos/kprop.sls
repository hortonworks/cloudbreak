remove_reserved_kerberos_ports:
  file.absent:
    - name: /etc/portreserve

stop_portreserve:
  service.dead:
    - name: portreserve

start_kpropd:
  service.running:
    - enable: True
    - name: kpropd