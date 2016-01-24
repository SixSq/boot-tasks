(ns sixsq.boot-rpm-impl-test
  (:require [clojure.test :refer :all]
            [sixsq.boot-rpm-impl :refer :all]
            [schema.core :as s])
  (:import (org.redline_rpm.header Flags Architecture Os)
           (org.redline_rpm.payload Directive)))

(deftest op-conversion-test
  (is (= Flags/EQUAL (op->int :eq)))
  (is (= 0 (op->int :BAD 0)))
  (is (nil? (op->int :BAD))))

(deftest directive-conversion-test
  (is (= Directive/CONFIG (kw->directive :config)))
  (is (= Directive/NONE (kw->directive :bad Directive/NONE)))
  (is (nil? (kw->directive :BAD nil))))

(deftest architecture-test
  (is (= Architecture/NOARCH (architecture :noarch)))
  (is (thrown? Exception (architecture :bad-arch))))

(deftest os-test
  (is (= Os/LINUX (os :linux)))
  (is (thrown? Exception (os :bad-os))))

(deftest op-schema
  (is (s/validate op :eq))
  (is (thrown? Exception (s/validate op :BAD))))

(deftest directive-schema
  (is (s/validate directive :config))
  (is (thrown? Exception (s/validate directive :BAD))))

(deftest constraint-schema
  (is (s/validate constraint ["a"]))
  (is (s/validate constraint ["a" :eq "1.2"]))
  (is (thrown? Exception (s/validate constraint "BAD")))
  (is (thrown? Exception (s/validate constraint ["a" "b"])))
  (is (thrown? Exception (s/validate constraint ["a" :BAD "c"]))))

(deftest minimal-rpm-spec-test
  (let [spec {:output-directory "ok"

              :description      "ok"
              :distribution     "ok"
              :group            "ok"
              :license          "ok"
              :summary          "ok"
              :url              "ok"
              :vendor           "ok"

              :package-name     "ok"
              :package-version  "ok"
              :package-release  "ok"}]
    (is (thrown? Exception (s/validate rpm-spec-schema spec)))
    (is (s/validate rpm-spec-schema (merge rpm-spec-defaults spec)))
    (is (s/validate rpm-spec-schema (merge rpm-spec-defaults spec {:requires [["a" :eq "1.0"]]})))))



