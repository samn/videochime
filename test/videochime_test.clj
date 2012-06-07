(ns videochime-test
  (:require [videochime :refer :all]
            [midje.sweet :refer :all]))

(tabular
  (fact "build-url"
    (build-url ?resource ?id) => ?expected)
  ?resource   ?id   ?expected
  nil   nil   (str url-base *account-id*)
  nil   1     (str url-base *account-id*)
  :video nil  (str url-base *account-id* "/video/")
  :video 1    (str url-base *account-id* "/video/1"))
