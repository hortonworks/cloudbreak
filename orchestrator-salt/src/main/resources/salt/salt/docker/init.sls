{% if salt['pillar.get']('docker:enableContainerExecutor') %}

install-docker-daemon:
  pkg.installed:
    - name: docker

launch-docker-daemon:
  service.running:
    - name: docker
    - require:
      - pkg: docker

{% endif %}