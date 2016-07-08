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
(ns sixsq.boot-deputil-impl
  (:require
    [boot.core :as boot]
    [boot.util :as butil]
    [clojure.edn :as edn]))

(defn defaults-map
  "Returns a map of dependencies (represented as a map) keyed by
   the project name."
  [deps]
  (->> deps
       (map butil/dep-as-map)
       (map (juxt :project identity))
       (into {})))

(defn read-deps [fileset fname]
  (if-let [f (boot/tmp-get fileset fname)]
    (->> f
         boot/tmp-file
         slurp
         edn/read-string)
    []))

(defn read-defaults [fileset fname]
  (defaults-map (read-deps fileset fname)))

(defn resolve-version
  "Looks up the value of :version in the map and replaces it
   by the value in the environment, if found."
  [m]
  (let [version (:version m)
        version (or (boot/get-env version) version)]
    (assoc m :version version)))

(defn remove-nil-values [m]
  (into {} (filter second m)))

(defn complete
  "Completes the information in the given dependency with
   the information from the defaults."
  [defaults dep]
  (let [{:keys [project] :as depmap} (butil/dep-as-map dep)
        result (resolve-version (merge (get defaults project)
                                       (remove-nil-values depmap)))]
    (butil/map-as-dep result)))

(defn merge-defaults [defaults deps]
  (vec (map (partial complete defaults) deps)))

(defn deps-update-function [completed-deps]
  (fn [existing-deps]
    (vec (concat existing-deps completed-deps))))

(defn update-deps! [completed-deps]
  (boot/set-env! :dependencies (deps-update-function completed-deps)))
