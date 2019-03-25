{% set timeout = salt['pillar.get']('recipes:timeout') %}
create_recipe_log_dir_post_cluster:
  file.directory:
    - name: /var/log/recipes/post-cluster-install
    - makedirs: True

{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}
{% if args['post-cluster-install'] is defined %}
{% for script_name in args['post-cluster-install'] %}
/opt/scripts/post-cluster-install/{{ script_name }}:
  file.managed:
     - source:
       - salt://post-recipes/scripts/{{ script_name }}
       - salt://post-recipes/scripts/post-date.sh
     - makedirs: True
     - mode: 755

run_post_cluster_install_script_{{ script_name }}:
  cmd.run:
    - name: /opt/scripts/recipe-runner.sh post-cluster-install {{ script_name }}
    - onlyif:
      - test -f /opt/scripts/post-cluster-install/{{ script_name }}
      - test ! -f /var/log/recipes/post-cluster-install/{{ script_name }}.success
    - timeout: {{ timeout }}
{% endfor %}
{% endif %}

{% endif %}
{% endfor %}

{% if "manager_server" in grains.get('roles', []) %}
{% if salt['pillar.get']('ldap', None) != None and salt['pillar.get']('ldap:local', None) == None %}

stop_cmserver:
  service.dead:
    - name: cloudera-scm-server
    - onlyif:
      - test ! -f /var/cmserver-restarted

start_cmserver:
  service.running:
    - enable: True
    - name: cloudera-scm-server
    - onlyif:
      - test ! -f /var/cmserver-restarted

/var/cmserver-restarted:
  file.touch:
    - onlyif:
      - test ! -f /var/cmserver-restarted

{% endif %}
{% endif %}
