# jeo - A Java Geo Library

## Building jeo

Before building jeo a few upstream libraries must first be built and installed in the 
local maven repository.

### Build proj4j

Build the ``jeo`` branch of the [proj4j repository](https://github.com/jdeolive/proj4j).

    git clone https://github.com/jdeolive/proj4j.git
    cd proj4j
    git checkout jeo
    mvn install

### Build jeo

jeo requires the [protoc](http://code.google.com/p/protobuf/downloads/list) program. On OSX, it is best to build it from source with `configure && make && sudo make install`. On Linux, it can usually be found in your distribution's package manager (Ubuntu: `sudo apt-get install protobuf-compiler`; Arch Linux: `sudo pacman -S extra/protobuf`).

    git clone https://github.com/jdeolive/jeo
    cd jeo
    mvn install
