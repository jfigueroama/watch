# watch

Little library based on Hawk (Clj) and Nodejs (Cljs) to reload namespaces
or run custom code when a file is modified.
It helps during development time. Also it includes other basic functions
to observe files and do things when they change.

[![Clojars Project](https://img.shields.io/clojars/v/jfigueroama/watch.svg)](https://clojars.org/jfigueroama/watch)

## Usage

For now, please see the docs in the functions or explore the source code.

```clojure
(require '[watch.core :as watch])
; This will reload your some.core every time the source file changes and also will print Hello and Bye. It prints the messages some time after the first file modification. It use an internal flag to know when the last change happened and will trigger the custom code/reload after some milliseconds to ensure the file is saved.
(watch/reload (require '[some.core :as c]) "./src/some/core..clj" (println "Hello") (println "Bye"))
(watch/watch "./test/watch/core_test.clj" (fn [ctx e] (run-tests 'watch.core)))
```

## License

Copyright © 2016 José Figueroa Martínez
All rights reserved.

BSD 2-Clause License
