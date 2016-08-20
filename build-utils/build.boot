(def +version+ "0.1.4")

(task-options!
  pom {:project     'sixsq/build-utils
       :version     +version+
       :description "common functions and tasks for managing builds"
       :url         "https://github.com/sixsq/boot-tasks"
       :license     {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0"}
       :scm         {:url "git@github.com:SixSq/boot-tasks.git"}}
  aot {:all true}
  push {:gpg-user-id "SixSq Release Manager <admin@sixsq.com>"})

(set-env!
 :resource-paths #{"src"}
 :source-paths #{"test" "test-resources"}

 :dependencies
 '[[org.clojure/clojure "1.8.0"]
   [boot/core "2.6.0"]
   [boot/pod "2.6.0"]
   [adzerk/boot-test "1.1.2" :scope "test"]
   [adzerk/bootlaces "0.1.13" :scope "test"]
   [tolitius/boot-check "0.1.3" :scope "test"]]

 :repositories
 #(reduce conj %
          '[["boot-releases" {:url "http://nexus.sixsq.com/content/repositories/releases-boot"}]
            ["boot-snapshots" {:url "http://nexus.sixsq.com/content/repositories/snapshots-boot"}]]))

(require
 '[adzerk.boot-test :refer [test]]
 '[adzerk.bootlaces :refer :all]
 '[tolitius.boot-check :refer [with-yagni
                               with-eastwood
                               with-kibit
                               with-bikeshed]])

(bootlaces! +version+)

(deftask build
         "full build and install"
         []
         (comp (pom) (test) (jar) (install)))
