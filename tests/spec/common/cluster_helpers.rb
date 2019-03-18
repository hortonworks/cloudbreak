RSpec.shared_context "shared cluster helpers", :a => :b do
  before { @some_var = :some_value }

  def status_check(cb, cluster_name)
    return get_cluster_json(cb, cluster_name)["status"]
  end

  def status_check_cluster(cb, cluster_name)
    return get_cluster_json(cb, cluster_name)["cluster"]["status"]
  end  

  def failure_checker(json)
    status=json["status"]
    cluster_status=json["cluster"]["status"]
    if status.include? "FAILED" or cluster_status.downcase.to_s.include? "failed"
      puts json["statusReason"]
      return json["statusReason"]
    else
      return ""
    end
  end

  def get_cluster_json(cb, cluster_name)
    result = cb.cluster.describe.name(cluster_name).build(false)
    if result.stdout.empty?
      return ""
    end      
    json_output = JSON.parse(result.stdout) 
    return json_output
  end              

  def wait_for_status(cb, cluster_name, status, cnt_max = 100, sleep_duration = 15)
    puts "Waiting for status: " + status + "..."
    sleep 5
    cnt=0
    json = get_cluster_json(cb, cluster_name)
    current_status = json["status"]
    status_reason = failure_checker(json)
    while (cnt < 100 and status != current_status and !status_reason.to_s.downcase.include? "failed") do
      current_status = status_check(cb, cluster_name)
      sleep 15
      cnt=cnt + 1
    end

    if status_reason.to_s.empty?
      return current_status
    else 
      return status_reason
    end
  end

  def wait_for_status_cluster(cb, cluster_name, status, cnt_max = 100, sleep_duration = 15)
    puts "Waiting for cluster status: " + status + "..."
    sleep 5
    cnt=0
    json = get_cluster_json(cb, cluster_name)
    current_status = json["cluster"]["status"]
    status_reason = failure_checker(json)
    while (cnt < 100 and status != current_status and !status_reason.to_s.downcase.include? "failed") do
      current_status = status_check_cluster(cb, cluster_name)
      sleep 15
      cnt=cnt + 1
    end

    if status_reason.to_s.empty?
      return current_status
    else 
      return status_reason
    end
  end    

  def wait_for_cluster_deleted(cb, cluster_name, cnt_max = 50, sleep_duration = 15)
    cnt=0
    current_status=true
    while (cnt < cnt_max and current_status) do
      result = list_with_name_exists(cluster_name) do
        cb.cluster.list.build 
      end
      current_status = result[0]   
      sleep sleep_duration
      cnt=cnt + 1
    end
    return current_status
  end 

  def skip_if(cb, cluster_name, status, reason)
    if status_check(cb, cluster_name) != status 
      skip reason
    end
  end

   def skip_if_cluster(cb, cluster_name, reason)
    if cluster_exists(cb, cluster_name)
      skip reason
    end  
  end   

  def get_node_count(cb, cluster_name, node_type)
    json = get_cluster_json(cb, cluster_name)
    json['instanceGroups'].each do |s|
      if  s['name'] == node_type
        return s['nodeCount']
      end
    end
  end

  let(:shared_let) { {'arbitrary' => 'object'} } 
  subject do
    'this is the subject'
  end
end