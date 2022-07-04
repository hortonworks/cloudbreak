{% set timeout = salt['pillar.get']('recipes:timeout') %}
create_recipe_log_dir_post_service_deployment:
  file.directory:
    - name: /var/log/recipes/post-service-deployment
    - makedirs: True

{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}
{% if args['post-service-deployment'] is defined %}
{% for script_name in args['post-service-deployment'] %}
/opt/scripts/post-service-deployment/{{ script_name }}:
  file.managed:
     - source:
       - salt://post-recipes/scripts/{{ script_name }}
       - salt://post-recipes/scripts/post-date.sh
     - makedirs: True
     - mode: 700

run_post_cluster_install_script_{{ script_name }}:
  cmd.run:
    - name: /opt/scripts/recipe-runner.sh post-service-deployment {{ script_name }}
    - onlyif:
      - test -f /opt/scripts/post-service-deployment/{{ script_name }}
      - test ! -f /var/log/recipes/post-service-deployment/{{ script_name }}.success
    - timeout: {{ timeout }}
{% endfor %}
{% endif %}

{% endif %}
{% endfor %}

{% if "ipa_member" in grains.get('roles', []) and "namenode" in grains.get('roles', []) %}
/opt/scripts/post-service-deployment/createuserhome.sh:
  file.managed:
    - source:
        - salt://post-recipes/scripts/createuserhome.sh
    - makedirs: True
    - mode: 755

createusername-cron:
  cron.present:
    - name: /opt/scripts/post-service-deployment/createuserhome.sh >> /var/log/createusername.log 2>&1
    - user: root
    - minute: '*/5'
    - require:
        - file: /opt/scripts/post-service-deployment/createuserhome.sh
{% endif %}