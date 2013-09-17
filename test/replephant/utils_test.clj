(ns replephant.utils-test
  (:use (midje sweet [util :only [testable-privates]])
        replephant.utils))

(testable-privates replephant.utils str->number)

(fact "`str->number` returns non-string input as is."
      (str->number nil) => nil
      (str->number 1.23) => 1.23
      (str->number 5) => 5)

(fact "`str->number` returns non-number strings as is."
      (str->number "") => ""
      (str->number "foo") => "foo"
      (str->number "d1234") => "d1234"
      (str->number "0-2") => "0-2"
      (str->number ".") => "."
      (str->number "0.") => "0."
      (str->number ".0") => ".0")

(fact "`str->number` converts number-like strings into numbers."
      (str->number "1") => 1
      (str->number "0") => 0
      (str->number "0.0") => 0.0
      (str->number "0.1") => 0.1
      (str->number "20.5") => 20.5
      (str->number "012345") => 012345
      (str->number "012345.6789") => 012345.6789
      (str->number "0x015") => 21)

(fact "`numberify-values` converts string values looking like numbers in the map to their corresponding numbers"
      (numberify-values {:irrelevant "2"}) => {:irrelevant 2}
      (numberify-values {:irrelevant "2", :foo "bar"}) => {:irrelevant 2, :foo "bar"}
      (numberify-values {:foo "bar", :quux "labs"}) => {:foo "bar", :quux "labs"}
      (numberify-values {:foo "bar", :quux nil}) => {:foo "bar", :quux nil}
      (numberify-values {:foo "1"}) => {:foo 1}
      (numberify-values {:foo "0"}) => {:foo 0}
      (numberify-values {:foo "0.0"}) => {:foo 0.0}
      (numberify-values {:foo "0.1"}) => {:foo 0.1}
      (numberify-values {:foo "20.5"}) => {:foo 20.5}
      (numberify-values {:foo "012345"}) => {:foo 012345}
      (numberify-values {:foo "012345.6789"}) => {:foo 012345.6789}
      (numberify-values {:foo "0x015"}) => {:foo 21}
      (numberify-values {}) => {}
      )
