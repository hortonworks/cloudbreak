
{{ salt['user.info'](salt['pillar.get']('userssh:user')).home }}/.ssh/authorized_keys:
  file.managed:
    - replace: True
    - contents_pillar: userssh:publickey