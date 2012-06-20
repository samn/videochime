# videochime
A passive gauge of video performance

## Usage
    lein deps
    lein run min-delay max-delay
the chime will poll for new data randomly between min & max milliseconds apart
min-delay should be at least 3 seconds (the default chiming length)


## License

Copyright © 2012 samn

Distributed under the Eclipse Public License, the same as Clojure.