(ns watch.core
  "Allos you to (mainly) reload your namespaces in an simple way or do other
  things when your files are modified."
  (:require [hawk.core :as hawk]
            [clojure.string :as st])
  (:import [java.io File]))

(defonce watched-files (atom {}))
(def enough-millis 1000)
(def waiting-millis 300)


(defn ensure-file
  ""
  [f]
  (if (not (.exists (File. f)))
    (throw (Exception.
             (str "File '" f "' doesn't exists and can't be watched!")))))

(defn now
  []
  (.getTimeInMillis (java.util.Calendar/getInstance)))

(defn watch-entry
  [f watcher]
  {:file f
   :descriptor watcher
   :last-call (atom (now))})

(defn watched
  "Return the watched file names from the user hashmap."
  []
  (set (keys @watched-files)))

(defn watched?
  "Tells if a file is being watched."
  [f]
  (some? (get (watched) f)))

(defn stop
  "Stop a watch using the watch descriptor or using
  a hashmap of descriptors (gotten from watch fn) and
  the file name to stop watching." 
  [f]
  (ensure-file f)
  (let [entry (get @watched-files f)]
    (if entry
      (do
        (hawk/stop! (:descriptor entry))
        (swap! watched-files dissoc f)
        (println "watched: " @watched-files)
        f))))

(defn stop-all
  "Stop all watching from a hashmap of descriptors."
  []
  (doseq [w (watched)]
    (stop w)))

(defn watch-debug
  "Observes all events from file name and print it on the console.
   Return the watch descriptor."
  [f]
  (if (watched? f)
    (ensure-file f))
  (let [watcher (hawk/watch!
                  [{:paths [f]
                    :context (constantly 0)
                    :handler (fn wfa-handler [ctx e]
                               (do
                                 (println (str "Event on " f ":\n")
                                          ctx
                                          e)
                                 (inc ctx)))}])]
    (swap! watched-files #(assoc % f (watch-entry f watcher)))
    f))

(defn watch
  "Observes a file  and executes a handler.
  Receives a hashmap to associate the watch descriptor using the
  filename as key.
  The handler function receives the ctx and the event descriptor.
  The handler are dispatched ONLY when the file is modified.
  You can only define a watch per file, as the watch descriptor is
  associated in the hm by key. If you need to associate many watches
  to a file, it is better to use hawk directly.

  Ej.
  (watch \"test.clj\" (fn wtestwt [ctx e] nil))
  "
  [f hn]
  (ensure-file f)
  (stop f)
  (let [watcher
        (hawk/watch!
          [{:paths [f]
            :context (constantly 0)
            :handler (fn wf-handler [ctx e]
                       (let [entry (get @watched-files f)
                             last-call-atom (:last-call entry)
                             last-call @last-call-atom
                             rnow (now)]
                         (if (>= (- rnow last-call) enough-millis)
                           (do
                             (reset! last-call-atom rnow)
                             (future
                               (Thread/sleep waiting-millis)
                               (try
                                 (hn ctx e)
                                 (catch Exception e
                                   (println (.getMessage e) "\n")))))))
                       (inc ctx)) }])]
    (swap! watched-files #(assoc % f (watch-entry f watcher)))
    f))


(defn ns-to-filename
  [require-def options]
  (let [extension    (or (if (:ext options) (name (:ext options)))
                         "clj")
        src-dir      (or (:src options) "src")

        myns       (str (first require-def))
        cleaned-ns (clojure.string/replace myns #"-" "_")
        path-ns    (st/join (java.io.File/separator)
                            (concat [src-dir] (st/split cleaned-ns #"\.")))
        file-name  (str path-ns "." extension)]
    file-name))

(defmacro reload
  "Load and reload a namespace using a namespace definition and optional parameters and actions.
  It executes all actions every time the namespace file changes, and prints \"Reloading \" with your namespace definition.
  It uses 'require' to do the job and if you are having trouble you can use macroexpand-1 too see if it is generating the
  correct file name to watch.
  
  (reload [watch.test-ns :as tns])
  (reload [watch.test-ns :as tns] {:ext :cljs :src \"opt-src\"})
  (reload [watch.test-ns :as tns] (println :a) (println :b))
  (reload [watch.test-ns :as tns] {:ext :cljs :src \"opt-src\"} (println :b) (println :c))"
  [require-def & actions]
  (if (vector? require-def)
    (let [faction   (first actions)
          commands  (if (map? faction) (rest actions) actions)
          options   (if (map? faction) faction {})
          file-name (ns-to-filename require-def options)]
      (let [rrou1 `(require (quote ~require-def))
            rroun `(require (quote ~require-def) :reload)
           prrou  `(println "Reloading " (quote ~require-def))]
       `(do
          ~rrou1
          (watch ~file-name (fn [ctx# e#]
                              ~rroun
                              ~prrou
                              ~@commands)))))))

(comment
  (short-reload [watch.test-ns :as tns] (println "holja"))
  (short-reload [watch.test-ns :as tns])
  (short-reload [watch.test-ns :as tns] {:ext "cljs"})
  (short-reload [watch.test-ns :as tns] {:ext :cljs :src "opt-src"})
  )

