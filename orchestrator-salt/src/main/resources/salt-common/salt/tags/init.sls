/var/lib/cdp:
  file.directory:
    - name: /var/lib/cdp
    - user: "root"
    - group: "root"
    - mode: '0644'

/var/lib/cdp/cdp_info.json:
   file.managed:
    - source: salt://tags/template/cdp_info.json.j2
    - template: jinja
    - user: "root"
    - group: "root"
    - mode: '0644'

