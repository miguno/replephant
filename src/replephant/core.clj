(ns replephant.core
  (:require (replephant [utils :as utils]
                        [data-sets :as ds])
            (clojure [string :as str]))
  (:use (replephant parser predicates)))

; TODO: how to handle missing job keys (e.g. for jobs that do not have a mapred.reduce.tasks property)
; TODO: use actual number of map/reduce tasks launched based on job history data instead of job configuration data
; TODO: add users->tools and/or tools->users functions
; TODO: add jobs-by-data-set function

;;;; Constants

(def ^:const ^:private unknown-data-set-identifier
  '("UNKNOWN DATA SET"))

(def ^:const ^:private missing-input-data-identifier
  '("NO JOB INPUT DATA CONFIGURED (E.G. WRITE-ONLY JOB) OR COULD NOT DETECT INPUT DATA"))

;;;; Predicates

(declare job->data-sets)

(defn unknown-input-data?
  "Returns true if the job's input data information could not be matched with any known data set."
  [data-sets job]
  (= unknown-data-set-identifier (job->data-sets job data-sets)))

;;;; Descriptive Statistics

(defn- sum-of-option
  "Computes the sum of the option across all provided jobs.  The value of the option must be a number (e.g.
  :mapred.map.tasks is an option that has number values)."
  [k jobs]
  (apply + (map k jobs)))

(defn- total-tasks
  "Computes the total number of tasks launched by the provided jobs."
  [jobs]
  (let [map-tasks (sum-of-option :mapred.map.tasks jobs)
        reduce-tasks (sum-of-option :mapred.reduce.tasks jobs)]
    (+ map-tasks reduce-tasks)))

(defn- num-tasks-by
  "Returns a map of the total number of tasks in jobs keyed by the result of f on each element.
  In other words, it groups jobs according to f and returns a map that maps each group to its corresponding total task
  count."
  [f jobs]
  (let [grouped (group-by f jobs)
        ks (map first grouped)
        counts (map (comp total-tasks second) grouped)]
    (zipmap ks counts)))

(defn tasks-by-user
  "Returns a map that maps Hadoop users to the number of tasks (maps plus reduces) they launched."
  [jobs]
  (num-tasks-by :user.name jobs))

(defn jobs-by-user
  "Returns a map that maps Hadoop users to the number of jobs they launched."
  [jobs]
  (let [grouped (group-by :user.name jobs)]
    (into {} (map #(vector (key %) (count (val %))) grouped))))

(defn- tool [job]
  (cond
   (hive? job) :hive
   (mahout? job) :mahout
   (pig? job) :pig
   (streaming? job) :streaming
   :else :other))

(defn tasks-by-tool
  "Returns a map that maps MapReduce tools (e.g. Hive) to the total number of tasks (maps plus reduces) launched by
  that tool."
  [jobs]
  (num-tasks-by tool jobs))

(defn jobs-by-tool [jobs]
  "Returns a map that maps MapReduce tools (e.g. Pig, Hive) to the number of jobs that were run with them."
  (let [num-all-jobs (count jobs)
        num-hive-jobs (count (filter hive? jobs))
        num-mahout-jobs (count (filter mahout? jobs))
        num-pig-jobs (count (filter pig? jobs))
        num-streaming-jobs (count (filter streaming? jobs))
        num-other-jobs (- num-all-jobs num-hive-jobs num-mahout-jobs num-pig-jobs num-streaming-jobs)]
    { :hive num-hive-jobs
      :mahout num-mahout-jobs
      :pig num-pig-jobs
      :streaming num-streaming-jobs
      :other num-other-jobs }))

; TODO: use an existing API to implement this function instead of manually implementing it
(defn- remove-scheme-and-authority [s]
  (str/replace s #"^[a-zA-Z]+://[^/]*/" "/"))

(defn- uri->data-sets [s data-sets]
  (let [path (remove-scheme-and-authority s)
        matching-data-sets (map first (filter #(re-find (val %) path) data-sets))]
    (distinct matching-data-sets)))

(defn- split-input-data-param [s]
  (if (str/blank? s) '("") (map str/trim (str/split s #","))))

(defn- input-data-param->data-sets
  [s data-sets]
  (let [uris (split-input-data-param s)]
    (distinct (mapcat #(uri->data-sets % data-sets) uris))))

(defn- job->data-sets
  [job data-sets]
  (let [input-data-params (input-data-params-of job)]
    (if (empty? input-data-params)
      ; can be empty for e.g. write-only jobs, DistCp, some Pig jobs
      missing-input-data-identifier
      (let [ds-coll (apply concat (map #(input-data-param->data-sets % data-sets) input-data-params))]
        (if (empty? ds-coll) unknown-data-set-identifier ds-coll)))))

(defn tasks-by-data-set
  "Returns a map that maps data sets to the number of tasks (maps plus reduces) launched against those data sets.
  Note that a MapReduce job may read from more than one data set, so the combined number of tasks in the returned map
  may be greater than the total number of tasks that were actually run in the cluster (as e.g. returned by
  tasks-by-user)."
  [jobs data-sets]
  (let [coll-of-ds-colls (map #(job->data-sets % data-sets) jobs)
        tasks-coll (map #(+ (:mapred.map.tasks %) (:mapred.reduce.tasks %)) jobs)
        coll-of-data-set->num-tasks (for [pair (map vector coll-of-ds-colls tasks-coll)]
          (let [data-sets-of-job (first pair)
                num-tasks (second pair)]
            ; TODO / FIXME: verify whether this use of map/into is always producing correct results
            (into {} (map #(vector % num-tasks) data-sets-of-job))))]
    (apply merge-with + coll-of-data-set->num-tasks)))

;;;; Display

(defn fingerprint
  ([job] (fingerprint job [:mapred.job.name :user.name :mapred.map.tasks :mapred.reduce.tasks :job-status]))
  ([job ks] (select-keys job ks)))
