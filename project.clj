(defproject replephant "0.8.0-SNAPSHOT"
  :description
    "A Clojure library to perform interactive analysis of Hadoop cluster usage via REPL and to generate usage reports."
  :url "https://github.com/miguno/replephant"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :scm {:name "git" :url "https://github.com/miguno/replephant"}
  :min-lein-version "2.0.0"
  :dependencies [
    [org.clojure/clojure "1.5.1"]
    [com.github.kyleburton/clj-xpath "1.4.1"]
    [org.apache.hadoop/hadoop-core "1.2.1"]
    [incanter "1.5.4"]]
  :profiles {:dev {:dependencies [[midje "1.5.0"]]
                   :plugins [[lein-midje "3.0.1"]]}}
  :jvm-opts ["-Xmx1g"]
  :main replephant.core)
