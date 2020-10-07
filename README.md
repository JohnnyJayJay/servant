# servant

Super simple HTTP file server/browser with basic authentication.

## Prerequisites

You will need [Leiningen][https://leiningen.org] 2.0.0 or above installed.

## Configuration

Place a single file called `config.edn` in the root directory that contains username, password and file root.
See [the example config file](./config.edn.example) for reference.

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2020 JohnnyJayJay
