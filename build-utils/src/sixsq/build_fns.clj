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
(ns sixsq.build-fns
  (:require
    [boot.core :as boot :refer [deftask]]
    [boot.pod :as pod]
    [sixsq.build-fns-impl]))

(defn merge-defaults
  "This function merges version, scope, and other information provided
   in a list of default dependency specifications with the partial
   dependency specifications in the second argument. The default
   dependency information is taken from the 'default-deps-dep'
   specification.  The referenced package must provide a file called
   'default-deps.edn' at the root of the jar file.  See the README
   for more information."
  [default-deps-dep deps]
  (-> (update-in (boot/get-env) [:dependencies] conj default-deps-dep)
      (pod/make-pod)
      (pod/with-call-in (sixsq.build-fns-impl/merge-deps ~deps))))

