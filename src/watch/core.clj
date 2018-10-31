(ns watch.core
  "Allos you to (mainly) reload your namespaces in an simple way or do other
  things when your files are modified."
  (:require [hawk.core :as hawk])
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

(defmacro reload
  "Reload a namespace every time a file is modified. It can also dispatch
   multiple actions. The macro returns the watch descriptor alone.
   If the reloading fails the actions are not executed.
  
  Ej.
  (def b
     (reload (require '[clojure.string :as st])
             \"t.clj\" (println \"hola\") (println \"adios\")))"
  [rou file & actions]
  (let [rrou (seq (conj (vec rou) :reload))
        prrou (list 'println (list 'quote rou))]
    `(do
       ~rrou
       (watch ~file (fn [ctx# e#]
                      ~rrou
                      ~prrou
                      ~@actions)))))


