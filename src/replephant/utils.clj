(ns replephant.utils
  (:import (java.io File)))

(defn sort-by-value-desc
  "Returns the map sorted by its values in descending order"
  [m]
  (into (sorted-map-by (fn [key1 key2]
                         (compare [(get m key2) key2]
                                  [(get m key1) key1]))) m))

(defn- str->number
  "If string looks like a number, returns string converted to the number.  Otherwise returns string as is."
  [s]
  (if (and (string? s) (re-find #"^[0-9]+(?:[\.x][0-9]+)?$" s)) (read-string s) s))

(defn- convert-values [f m]
  (let [vs (map f (vals m))
        ks (keys m)]
    (zipmap ks vs)))

(defn numberify-values
  "Converts string values looking like a number to the corresponding number.  Leaves other values untouched."
  [m]
  (convert-values str->number m))

(defn- str-true-false->boolean
  "If the string is \"true\" or \"false\", returns the corresponding boolean.  Otherwise returns string as is."
  [s]
  (cond
   (= "true" s) true
   (= "false" s) false
   :else s))

(defn booleanify-values
  "Converts \"true\" and \"false\" values to the corresponding boolean.  Leaves other values untouched."
  [m]
  (convert-values str-true-false->boolean m))

(defn find-files
  "Recursively finds files whose names match the regex, starting at the root directory."
  [regex root-dir]
  (let [files (file-seq (clojure.java.io/file root-dir))
        filenames (map (memfn File/getName) files)
        filename->file (zipmap filenames files)
        matching-filenames (filter #(re-find regex %) filenames)]
    (vals (select-keys filename->file matching-filenames))))
