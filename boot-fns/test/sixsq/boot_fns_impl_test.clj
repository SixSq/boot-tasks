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
(ns sixsq.boot-fns-impl-test
  (:require [clojure.test :refer :all]
            [sixsq.boot-fns-impl :refer :all]
            [boot.core :as boot]))

(def ^:const test-deps '[[alpha/beta "1.0.0"]
                         [gamma/delta "2.0.0" :a "a"]])

(def ^:const test-indexed-map {'alpha/beta  {:project 'alpha/beta
                                             :version "1.0.0"
                                             :scope   "compile"}
                               'gamma/delta {:project 'gamma/delta
                                             :version "2.0.0"
                                             :scope   "compile"
                                             :a       "a"}})

(deftest check-defaults-map
  (is (= test-indexed-map
         (defaults-map test-deps))))

(deftest check-read-default-deps
  (is (= test-deps (read-default-deps "test-default-deps.edn"))))

(deftest check-remove-nil-values
  (are [x y] (= x (remove-nil-values y))
             {} {:a nil}
             {:a 1} {:a 1 :b nil}
             {:a 1 :c 3} {:a 1 :b nil :c 3}))

(deftest check-resolve-version
  (let [v1 "1.2.3"
        v2 "3.2.1"
        _ (boot/set-env! :version v1 :other v2)]
    (are [x y] (= x (resolve-version y))
               {:a 1 :version v1} {:a 1 :version :version}
               {:a 1 :version v2} {:a 1 :version :other}
               {:a 1 :version :unknown} {:a 1 :version :unknown}
               {:version nil} {:version nil})))

(deftest check-complete-dep
  (let [defaults (defaults-map test-deps)
        f (partial complete-dep defaults)]
    (are [x y] (= x (f y))
               '[alpha/beta "1.0.0"] '[alpha/beta]
               '[alpha/beta "1.0.0" :b "b"] '[alpha/beta nil :b "b"]
               '[gamma/delta "2.0.0" :a "a"] '[gamma/delta])))

(deftest check-complete-deps
  (let [defaults (defaults-map test-deps)]
    (are [x y] (= x (complete-deps defaults y))
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
