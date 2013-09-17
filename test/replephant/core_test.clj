(ns replephant.core-test
  (:use (midje sweet [util :only [testable-privates]])
        replephant.core))

(testable-privates replephant.core remove-scheme-and-authority split-input-data-param)

(fact "`remove-scheme-and-authority` removes the scheme from an URI"
      (remove-scheme-and-authority "hdfs:///path/to/foo") => "/path/to/foo"
      (remove-scheme-and-authority "file:///foo/bar") => "/foo/bar")

(fact "`remove-scheme-and-authority` does not modify URIs that only contain path information"
      (remove-scheme-and-authority "/path/to/foo") => "/path/to/foo"
      (remove-scheme-and-authority "/a/../b") => "/a/../b"
      (remove-scheme-and-authority "a/b") => "a/b"
      (remove-scheme-and-authority "/C/..//D/x") => "/C/..//D/x"
      (remove-scheme-and-authority "/foo/bar") => "/foo/bar")

(fact "`split-input-data-param` converts URIs specified in mapred.input.dir into a a collection of those URIs"
      (split-input-data-param "/foo/bar,/quux/labs") => ["/foo/bar" "/quux/labs"]
      (split-input-data-param "hdfs:///a/b,file://host/C") => ["hdfs:///a/b" "file://host/C"]
      (split-input-data-param "/foo/bar") => ["/foo/bar"])

(fact "`split-input-data-param` removes leading and trailing whitespace of URIs"
      (split-input-data-param "  leading/whitespace") => ["leading/whitespace"]
      (split-input-data-param "trailing/whitespace ") => ["trailing/whitespace"]
      (split-input-data-param "   trailing/and/leading/whitespace ") => ["trailing/and/leading/whitespace"])

(def ^:private test-jobs [
           {
            :mapred.job.name "Twitter + Facebook analysis"
            :mapred.input.dir "/twitter/firehose/2013/05/10,/facebook/social-graph/2013/05/10"
            :mapred.map.tasks 10
            :mapred.reduce.tasks 20
            }
           {
            :mapred.job.name "Twitter analysis for 2013-04-01"
            :mapred.input.dir "hdfs:///twitter/firehose/2013/04/01"
            :mapred.map.tasks 1000
            :mapred.reduce.tasks 2000
            }
           {
            :mapred.job.name "Twitter analysis for 2013-04-30"
            :mapred.input.dir "hdfs://namenode.your.datacenter/twitter/firehose/2013/04/30"
            :mapred.map.tasks 50000
            :mapred.reduce.tasks 50000
            }
           {
            :mapred.job.name "Job using a data set that we have not configured"
            :mapred.input.dir "/google/user-profiles"
            :mapred.map.tasks 123456
            :mapred.reduce.tasks 123456
            }
           ])
