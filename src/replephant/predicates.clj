(ns replephant.predicates)

(defn pig?
  "Returns true if the job was a Pig job."
  [job]
  (contains? job :pig.version))

(defn hive?
  "Returns true if the job was a Hive job."
  [job]
  (contains? job :hive.exec.plan))

(defn streaming?
  "Returns true if the job was an Hadoop Streaming job."
  [job]
  (= "org.apache.hadoop.streaming.PipeMapRunner" (:mapred.map.runner.class job)))

(defn mahout?
  "Returns true if the job was a Mahout job."
  [job]
  (and (contains? job :mapreduce.map.class)
       (re-find #"^org.apache.mahout." (:mapreduce.map.class job))))

(defn distcp?
  "Returns true if the job was a DistCp job."
  [job]
  (contains? job :distcp.job.dir))

(defn compressed-output?
  "Returns true if the job was configured to write compressed output data."
  [job]
  (true? (:mapred.output.compress job)))

(defn successful?
  "Returns true if the job finished successfully."
  [job]
  (= "SUCCESS" (:job-status job)))

(defn failed?
  "Returns true if the job failed to finish successfully."
  [job]
  (not (successful? job)))

(defn missing-input-data?
  "Returns true if the job had no input data configured OR if the input data information could not be detected."
  [job]
  (true? (:missing-input-data (meta job))))

