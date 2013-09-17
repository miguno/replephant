(ns replephant.parser
  (:require [replephant.utils :as utils]
            (clojure [pprint :as pp]
                     [string :as str]))
  (:use clj-xpath.core
        replephant.predicates)
  (:import (java.io File FileInputStream)
           org.apache.hadoop.conf.Configuration
           (org.apache.hadoop.mapred DefaultJobHistoryParser JobHistory JobHistory$JobInfo JobHistory$Keys Counters)
           (org.apache.hadoop.fs FileSystem)))


; TODO: allow user to specify subset of relevant keys via :only when parsing jobs to reduce memory footprint
; TODO: provide :with-minimum-keys that extract all required keys from job data to run our statistics functions
; TODO: convert float/double data fields to their float/double counterparts in job data structures
; TODO: handle input data configuration for Pig jobs (pig.input.dirs or pig.inputs?), which seem to miss mapred.input.dir

;;;; Handling job configuration files

(def ^:private find-job-confs (partial utils/find-files #"^job_.*conf\.xml$"))

; TODO: how to handle Pig jobs that define only pig.inputs (with a value in a weird text encoding) but not mapred.input.dir?
; TODO: how to handle DistCP jobs (which only have distcp.dest.path but apparently no distcp.src.path)?
(defn input-data-params-of [job]
  (remove empty? ((juxt :mapred.input.dir :mapred.input.dir.mappers) job)))

(defn- parse-job-conf [filename]
  (let [job-conf (slurp filename)
        job-conf-doc (xml->doc job-conf)
        options (apply hash-map ($x:text* "//configuration/property/name|//configuration/property/value" job-conf-doc))
        m (zipmap (map keyword (keys options)) (vals options))
        job-without-metadata (->> m (utils/numberify-values) (utils/booleanify-values))]
    (if (empty? (input-data-params-of job-without-metadata))
      (vary-meta job-without-metadata assoc :missing-input-data true)
      job-without-metadata)))


;;;; Handling job history files

(def ^:private find-job-histories (partial utils/find-files #"^job_.*(?<!conf\.xml)$"))

(defn- parse-start-stop-failure-history
  "Returns a map containing all key-values read back from the job events logged in filename, which must refer to a job
  history file.  The only events available to this function are those related to job start, job finish or job failure.
  For background technical information see `org.apache.mapred.JobHistory.{JobInfo,DefaultJobHistoryParser}`."
  [filename]
  (let [filesystem (FileSystem/getLocal (Configuration.))
        ; The fake job id is only required to construct a job info object, whose values will be filled in next step.
        job-info (JobHistory$JobInfo. "FAKE_JOB_ID")
        ; Workaround because parseJobTasks modifies job-info in-place
        not-used (DefaultJobHistoryParser/parseJobTasks filename job-info filesystem)]
    (into {} (.getValues job-info))))

(defn- job-counters-of [history]
  (Counters/fromEscapedCompactString (history JobHistory$Keys/COUNTERS)))

(defn- counter-field->value [counters field group]
  (.getCounter (.findCounter counters group field)))

(defn- history-field->keyword
  "Converts a job history field to an idiomatic Clojure keyword (e.g. \"JOB_STATUS\" to :job-status"
  [s]
  (-> s (str/lower-case) (str/replace "_" "-") (keyword)))

(defn- parse-history-counters
  ([counters]
     (let [counter->group {
                        "FILE_BYTES_READ" "FileSystemCounters",
                        "FILE_BYTES_WRITTEN" "FileSystemCounters",
                        "HDFS_BYTES_READ" "FileSystemCounters",
                        "HDFS_BYTES_WRITTEN" "FileSystemCounters",
                        "DATA_LOCAL_MAPS" "org.apache.hadoop.mapred.JobInProgress$Counter",
                        "RACK_LOCAL_MAPS" "org.apache.hadoop.mapred.JobInProgress$Counter",
                        "REDUCE_SHUFFLE_BYTES" "org.apache.hadoop.mapred.Task$Counter",
                        }]
       (parse-history-counters counters counter->group)))
  ([counters counter->group]
     (let [fields (map #(history-field->keyword (key %)) counter->group)
           values (map #(counter-field->value counters (key %) (val %)) counter->group)]
       (zipmap fields values))))

(defn- extract-history-fields
  [history history-fields]
  (let [fields (map history-field->keyword history-fields)
        values (map #(history (JobHistory$Keys/valueOf %)) history-fields)
        m (zipmap fields values)]
    (->> m (utils/numberify-values) (utils/booleanify-values))))

(defn- parse-job-history
  ([filename]
     ; TODO: add "TOTAL_MAPS", "TOTAL_REDUCES", "FAILED_MAPS", "FAILED_REDUCES" (removed b/c missing for failed jobs)
     (let [history-fields ["JOB_STATUS" "USER" "SUBMIT_TIME" "LAUNCH_TIME" "FINISH_TIME" "JOB_PRIORITY" "JOB_QUEUE"
                            "JOBID" "JOBNAME"]]
       (parse-job-history filename history-fields)))
  ([filename history-fields]
     (let [history (parse-start-stop-failure-history filename)
           field->value (extract-history-fields history history-fields)]
       (if (successful? field->value)
         ; Counters are only available for successful jobs
         (merge (parse-history-counters (job-counters-of history)) field->value)
         field->value))))


;;;; Tying job configuration files and job history files together

(def ^:private job-id-grouper (partial re-find #"job_[0-9]+_[0-9]+"))

(defn load-jobs
  "Recursively loads job configuration and history files from local disk, starting at the given root directory."
  [root-dir]
  (let [conf-files (find-job-confs root-dir)
        history-files (find-job-histories root-dir)
        absolute-filenames (map (memfn File/getAbsolutePath) (concat conf-files history-files))
        jobid->file-pair (into {} (group-by job-id-grouper absolute-filenames))]
    (for [e jobid->file-pair]
      (let [job-id (key e)
            conf-file (first (val e))
            job-conf (let [conf (parse-job-conf conf-file)] (if (empty? conf) {} conf))
            history-file (second (val e))
            job-history (if (empty? history-file) {} (parse-job-history history-file))
            job (merge job-conf job-history)]
        (vary-meta job assoc :job-id job-id)))))
