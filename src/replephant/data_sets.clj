(ns replephant.data-sets)

(def data-sets
  "Maps each data set name (string) to a regex that we match against the dir/file paths extracted from URIs defined in
  mapred.input.dir and mapred.input.dir.mappers.  For example, a regex should target /path/to/foo/ but not
  hdfs:///path/to/foo/ or hdfs://namenode.your.datacenter/path/to/foo/."
  {
   "Twitter Firehose data" #"^/twitter/firehose/?"
   "Facebook Social Graph data" #"^/facebook/social-graph/?"
   })
