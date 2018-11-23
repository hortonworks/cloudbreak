Encoding.default_external = Encoding::UTF_8 
Encoding.default_internal = Encoding::UTF_8

if (defined? state == false)
  state = 0
end
if (state == nil and !$_.include?("<!DOCTYPE") and !$_.to_s.strip.empty?)
  puts $_
end
if ($_.include?("<!DOCTYPE"))
  state = 1
end
if (state == 1 and $_.include?("<body"))
  state = 2
end
if (state == 2 and $_ !~ /<[^>]+>/ and !$_.to_s.strip.empty?)
  puts $_
end