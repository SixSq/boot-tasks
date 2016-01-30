(ns sixsq.boot-depmgt-impl-test
  (:require [clojure.test :refer :all]
            [sixsq.boot-depmgt-impl :refer :all]))

(deftest check-prepend
  (are [x y] (= x (prepend-version-key y))
       [] []
       [:a "a"] [:a "a"]
       [:version "1.2.3"] ["1.2.3"]
       [:version "1.2.3" :a "a"] ["1.2.3" :a "a"]))

(deftest check-dep->entry
  (are [x y] (= x (dep->entry y))
       ['alpha/beta {}] ['alpha/beta]
       ['alpha/beta {:a "a"}] ['alpha/beta :a "a"]
       ['alpha/beta {:version "1.0.0"}] ['alpha/beta "1.0.0"]
       ['alpha/beta {:version "1.0.0" :a "a"}] ['alpha/beta "1.0.0" :a "a"]))

(deftest check-entry->dep
  (are [x y] (= x (apply entry->dep y))
       ['alpha/beta] ['alpha/beta {}] 
       ['alpha/beta :a "a"] ['alpha/beta {:a "a"}] 
       ['alpha/beta "1.0.0"] ['alpha/beta {:version "1.0.0"}]
       ['alpha/beta "1.0.0" :a "a"] ['alpha/beta {:version "1.0.0" :a "a"}]))

(deftest check-defaults-map
  (is (= {'alpha/beta {:version "1.0.0"}
          'gamma/delta {:version "2.0.0" :a "a"}}
         (defaults-map '[[alpha/beta "1.0.0"]
                         [gamma/delta "2.0.0" :a "a"]]))))

(deftest check-complete
  (let [defaults (defaults-map '[[alpha/beta "1.0.0"]
                                 [gamma/delta "2.0.0" :a "a"]])
        f (complete defaults)]
    (are [x y] (= x (f y))
         '[alpha/beta "1.0.0"] '[alpha/beta]
         '[alpha/beta "1.0.0" :b "b"] '[alpha/beta :b "b"]
         '[gamma/delta "2.0.0" :a "a"] '[gamma/delta])))

(deftest check-merge
  (let [defaults (defaults-map '[[alpha/beta "1.0.0"]
                                 [gamma/delta "2.0.0" :a "a"]])]
    (are [x y] (= x (merge-defaults defaults y))
         '[[alpha/beta "1.0.0" :b "b"]
           [gamma/delta "2.0.0" :a "a"]]

         '[[alpha/beta :b "b"]
           [gamma/delta]]

         '[[gamma/delta "2.0.0" :a "a"]
           [alpha/beta "1.0.0" :b "b"]]
         '[[gamma/delta]
           [alpha/beta :b "b"]]

         '[[gamma/delta "2.0.0" :a "a"]
           [alpha/beta "1.0.0" :b "b"]
           [alpha/omega]]
         '[[gamma/delta]
           [alpha/beta :b "b"]
           [alpha/omega]])))
