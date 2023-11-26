(defproject natto "0.1.0-SNAPSHOT"
  :description "Experimental static checking"
  :url "https://github.com/egydiopacheco/natto"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.match "1.0.1"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.3.1"]
                                  [org.clojure/tools.analyzer.jvm "1.2.3"]
                                  [criterium "0.4.6"]
                                  [instaparse "1.4.12"]
                                  [cider/cider-nrepl "0.30.0"]]
                   :repl-options {:init-ns natto.core
                                  :nrepl-middleware
                                  [cider.nrepl/wrap-apropos
                                   cider.nrepl/wrap-classpath
                                   cider.nrepl/wrap-clojuredocs
                                   cider.nrepl/wrap-complete
                                   cider.nrepl/wrap-debug
                                   cider.nrepl/wrap-format
                                   cider.nrepl/wrap-info
                                   cider.nrepl/wrap-inspect
                                   cider.nrepl/wrap-macroexpand
                                   cider.nrepl/wrap-ns
                                   cider.nrepl/wrap-spec
                                   cider.nrepl/wrap-profile
                                   cider.nrepl/wrap-refresh
                                   cider.nrepl/wrap-resource
                                   cider.nrepl/wrap-stacktrace
                                   cider.nrepl/wrap-test
                                   cider.nrepl/wrap-trace
                                   cider.nrepl/wrap-out
                                   cider.nrepl/wrap-undef
                                   cider.nrepl/wrap-version
                                   cider.nrepl/wrap-xref]}}})
