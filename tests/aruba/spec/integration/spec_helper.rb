require 'aruba/rspec'
require "rspec/json_expectations"
require 'json'
require_relative "../common/command_builder"
require "oauth"

def html_print(&blk)
  puts "<pre>"
  blk.call
  puts "</pre>"
end