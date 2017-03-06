ALERT ${alertName}
  IF 100 - (Hadoop_ResourceManager_AvailableMB{job="resourcemanager",name="QueueMetrics",q0="root",q1=""} / (Hadoop_ResourceManager_AvailableMB{job="resourcemanager",name="QueueMetrics",q0="root",q1=""} + Hadoop_ResourceManager_AllocatedMB{job="resourcemanager",name="QueueMetrics",q0="root", q1=""}) * 100) ${operator} ${threshold}
  FOR ${period}m
  LABELS {severity="critical"}
  ANNOTATIONS {description="The overall yarn memory usage exceeds the threshold {{ $value }}.", summary="Overall yarn memory usage is dangerously high."}