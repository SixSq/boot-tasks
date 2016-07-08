;; Copyright 2016, SixSq Sarl
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;; http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
(ns sixsq.boot-deputil-impl-test
  (:require [clojure.test :refer :all]
            [sixsq.boot-deputil-impl :refer :all]
            [boot.core :as boot]
            [boot.util :as butil]))

(deftest check-dep-as-map
  (are [x y] (= x (butil/dep-as-map y))
             {:project 'alpha/beta :version nil :scope "compile"} ['alpha/beta]
             {:project 'alpha/beta :version nil :scope "compile" :a "a"} ['alpha/beta nil :a "a"]
             {:project 'alpha/beta :version "1.0.0" :scope "compile"} ['alpha/beta "1.0.0"]
             {:project 'alpha/beta :version "1.0.0" :scope "compile" :a "a"} ['alpha/beta "1.0.0" :a "a"]))

(deftest check-map-as-dep
  (are [x y] (= x (butil/map-as-dep y))
             ['alpha/beta] {:project 'alpha/beta}
             ['alpha/beta :a "a"] {:project 'alpha/beta :version nil :a "a"}
             ['alpha/beta "1.0.0"] {:project 'alpha/beta :version "1.0.0"}
             ['alpha/beta "1.0.0" :a "a"] {:project 'alpha/beta :version "1.0.0" :a "a"}))

(deftest check-defaults-map
  (is (= {'alpha/beta {:project 'alpha/beta
                       :version "1.0.0"
                       :scope "compile"}
          'gamma/delta {:project 'gamma/delta
                        :version "2.0.0"
                        :scope "compile"
                        :a "a"}}
         (defaults-map '[[alpha/beta "1.0.0"]
                         [gamma/delta "2.0.0" :a "a"]]))))

(deftest check-complete
  (let [defaults (defaults-map '[[alpha/beta "1.0.0"]
                                 [gamma/delta "2.0.0" :a "a"]])
        f (partial complete defaults)]
    (are [x y] (= x (f y))
         '[alpha/beta "1.0.0"] '[alpha/beta]
         '[alpha/beta "1.0.0" :b "b"] '[alpha/beta nil :b "b"]
         '[gamma/delta "2.0.0" :a "a"] '[gamma/delta])))

(deftest check-lookup-keywords
  (let [_ (boot/set-env! :replace "REPLACE")
        x {:version "1.0.0"}
        y {:version :unknown}
        z {:version :replace}]
    (is (= x (resolve-version x)))
    (is (= y (resolve-version y)))
    (is (= {:version "REPLACE"} (resolve-version z)))))

(deftest check-merge
  (let [defaults (defaults-map '[[alpha/beta "1.0.0"]
                                 [gamma/delta "2.0.0" :a "a"]])]
    (are [x y] (= x (merge-defaults defaults y))
         '[[alpha/beta "1.0.0" :b "b"]
           [gamma/delta "2.0.0" :a "a"]]

         '[[alpha/beta nil :b "b"]
           [gamma/delta]]

         '[[gamma/delta "2.0.0" :a "a"]
           [alpha/beta "1.0.0" :b "b"]]
         '[[gamma/delta]
           [alpha/beta nil :b "b"]]

         '[[gamma/delta "2.0.0" :a "a"]
           [alpha/beta "1.0.0" :b "b"]
           [alpha/omega]]
         '[[gamma/delta]
           [alpha/beta nil :b "b"]
           [alpha/omega]])))
