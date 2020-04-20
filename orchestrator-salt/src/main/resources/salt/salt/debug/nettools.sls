install_network_packages:
  pkg.installed:
    - pkgs:
      - net-tools
      - wget
    - unless:
      - rpm -q net-tools wget