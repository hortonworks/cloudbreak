create_recipe_log_dir_pre_start:
  file.directory:
    - name: /var/log/recipes

{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}
{% for script_name in args['pre-ambari-start'] %}
/opt/scripts/pre-ambari-start/{{ script_name }}:
  file.managed:
     - source:
       - salt://pre-recipes/scripts/{{ script_name }}
       - salt://pre-recipes/scripts/pre-date.sh
     - makedirs: True
     - mode: 755

run_pre_ambari_start_script_{{ script_name }}:
  cmd.run:
    - name: sh -x /opt/scripts/pre-ambari-start/{{ script_name }} 2>&1 | tee -a /var/log/recipes/pre-ambari-start-{{ script_name }}.log && exit ${PIPESTATUS[0]}
    - onlyif:
      - ls /opt/scripts/pre-ambari-start/{{ script_name }}
    - unless: ls /var/log/recipes/pre-ambari-start-{{ script_name }}.log
{% endfor %}

{% endif %}
{% endfor %}