(task-options!
  pom {:project     'sixsq/boot-rpm
       :version     "0.0.1"
       :description "build RPM package using redline"
       :url         "https://github.com/sixsq/boot"
       :license     {"EPL" "https://www.eclipse.org/org/documents/epl-v10.html"}}
  aot {:all true})

(set-env!
 :resource-paths #{"src"}
 :source-paths #{"test"}
 :dependencies
 '[[org.clojure/clojure "1.8.0"]
   [boot/core "2.5.5"]
   [boot/pod "2.5.5"]
   [org.redline-rpm/redline "1.2.2"]
   [prismatic/schema "1.0.4"]
   [org.slf4j/slf4j-simple "1.7.13"]

   [adzerk/boot-test "1.1.0" :scope "test"]])

(require
 '[adzerk.boot-test :refer [test]])

(deftask build
         "full build and install"
         []
         (comp (pom) (test) (jar) (install)))
