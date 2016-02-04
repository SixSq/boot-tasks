(task-options!
  pom {:project     'sixsq/boot-deputil
       :version     "0.2.0-SNAPSHOT"
       :description "central management of dependency versions and flags"
       :url         "https://github.com/sixsq/boot-tasks"
       :license     {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0"}}
  aot {:all true})

(set-env!
 :resource-paths #{"src"}
 :source-paths #{"test"}

 :dependencies
 '[[org.clojure/clojure "1.8.0"]
   [boot/core "2.5.5"]
   [boot/pod "2.5.5"]
   [adzerk/boot-test "1.1.0" :scope "test"]]

 :repositories
 #(reduce conj %
          '[["boot-releases" {:url "http://nexus.sixsq.com/content/repositories/releases-boot"}]
            ["boot-snapshots" {:url "http://nexus.sixsq.com/content/repositories/snapshots-boot"}]]))

(require
 '[adzerk.boot-test :refer [test]])

(deftask build
         "full build and install"
         []
         (comp (pom) (test) (jar) (install)))
