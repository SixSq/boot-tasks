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
(ns sixsq.build-fns-impl
  (:require
    [boot.core :as boot]
    [boot.pod :as pod]
    [boot.util :as butil]
    [clojure.edn :as edn]
    [clojure.java.io :as io]))

(defn get-env
  "Defaults to using the global environment when available.  If
   not, then it will use the pod environment.  If neither exists,
   then nil will be returned."
  [k]
  (when-let [env (or (boot/get-env) pod/env)]
    (get env k)))

(defn protected-dep-as-map
  "Uses the standard boot utility to convert a dependency specification
   to a map.  However, if an exception is thrown in the conversion
   (usually from an invalid spec like ['myproject :scope \"test\"]),
   then a modified exception will be thrown showing the problematic
   specification."
  [dep]
  (try
    (butil/dep-as-map dep)
    (catch Exception _
      (throw (ex-info (str "invalid dependency specification: '" dep "'\n") {})))))

(defn defaults-map
  "Accepts a list of dependency specifications and returns a
   map where the values are the map representation of the dependency
   and the key is the value of :project in the dependency."
  [deps]
  (->> deps
       (map protected-dep-as-map)
       (remove nil?)
       (map (juxt :project identity))
       (into {})))

(defn read-default-deps
  "Reads a list of dependency specifications from the resource
   'default-deps.edn' which must be available on the classpath.
   Returns nil if the file cannot be found."
  ([]
   (read-default-deps nil))
  ([defaults-filename]
   (let [defaults-filename (or defaults-filename "default-deps.edn")]
     (when-let [f (io/resource defaults-filename)]
       (-> f
           slurp
           edn/read-string)))))

(defn remove-nil-values
  "Removes all keys in the map that have nil values."
  [m]
  (into {} (filter second m)))

(defn resolve-version
  "Looks up the value of :version key in the environment. If
   the value exists, it replaces the value in the map.  If not,
   the value is unchanged."
  [dep-map]
  (if-let [version (:version dep-map)]
    (assoc dep-map :version (or (get-env version) version))
    dep-map))

(defn complete-dep
  "Completes the information in the given dependency with
   the information from the defaults."
  [defaults dep]
  (let [{:keys [project] :as dep-map} (butil/dep-as-map dep)]
    (->> dep-map
         remove-nil-values
         (merge (get defaults project))
         resolve-version
         butil/map-as-dep)))

(defn complete-deps
  "Iterates over the list of dependencies, completing values
   from the defaults for each dependency."
  [defaults deps]
  (vec (map (partial complete-dep defaults) deps)))

(defn merge-deps [deps]
  (complete-deps (defaults-map (read-default-deps nil)) deps))
