get_free_space:
  cmd.run:
    - name: /usr/bin/df --exclude-type=tmpfs  --exclude-type=devtmpfs -P -l|grep " /$"|sed 's/  */ /g'|cut -f 4 -d ' '|sed -E 's/(.*)/"freeSpace":\1/g'
