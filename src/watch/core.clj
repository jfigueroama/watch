(ns watch.core
  "Allos you to (mainly) reload your namespaces in an simple way or do other
  things when your files are modified."
  (:require [hawk.core :as hawk]))

(defn watched
  "Return the watched file names from the user hashmap."
  [hm]
  (keys hm))

(defn stop-watch
  "Stop a watch using the watch descriptor or using
   a hashmap of descriptors (gotten from watch fn) and
   the file name to stop watching." 
  ([hm f]
   (do
     (hawk/stop! (get hm f))
     (dissoc hm f)))
  ([watchd]
   (hawk/stop! watchd)))

(defn stop-watched
  "Stop all watching from a hashmap of descriptors."
  [hm]
  (do
    (doseq [w (keys hm)]
      (stop-watch hm w))
    {}))

(defn watch-debug
  "Observes all events from file name and print it on the console.
   Return the watch descriptor."
  [f]
  (let [watcher (hawk/watch!
                  [{:paths [f]
                    :context (constantly 0)
                    :handler (fn wfa-handler [ctx e]
                               (do
                                 (println (str "Event on " f ":\n")
                                          ctx
                                          e)
                                 (inc ctx)))}])]
    watcher))

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
  (watch {} \"test.clj\" (fn wtestwt [ctx e] nil))
  "
  ([hm f hn]
   (let [ohn (get hm f)
         watcher (hawk/watch!
                   [{:paths [f]
                     :context (constantly 0)
                     :handler (fn wf-handler [ctx e]
                                (if (= (:kind e) :modify)
                                  (do
                                    (try
                                      (hn ctx e)
                                      (catch Exception e
                                        (println 
                                          (.getMessage e) "\n")))
                                    (inc ctx))
                                  (inc ctx)))}])]
     (assoc
       (if ohn
         (stop-watch hm f)
         hm)
       f watcher)))
  ([f hn]
   (get (watch {} f hn) f)))

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


