(ns replephant.predicates-test
  (:use replephant.predicates midje.sweet))

(fact "`pig?` can identify Pig jobs"
      (pig? {:pig.version anything}) => true
      (pig? {"pig.version" anything}) => false
      (pig? {:pig-version anything}) => false
      (pig? {:something anything :else anything}) => false
      (pig? {}) => false)

(fact "`hive?` can identify Hive jobs"
      (hive? {:hive.exec.plan anything}) => true
      (hive? {"hive.exec.plan" anything}) => false
      (hive? {:hive-exec-plan anything}) => false
      (hive? {:something anything :else anything}) => false
      (hive? {}) => false)
