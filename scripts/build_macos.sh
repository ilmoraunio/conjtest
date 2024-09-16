#!/bin/bash

set -x

DIRNAME="$(dirname $0)"

cd "$DIRNAME/.."

bb uberjar target/cljconf.jar -m cljconf.bb.main

#mkdir -p target/META-INF
#mkdir -p target/resources/META-INF
#cp bb.edn target/META-INF/
#cp bb.edn target/resources/META-INF
#(cd target && jar -uf cljconf.jar META-INF/ && jar -uf cljconf.jar resources/)

[ ! -f bb ] && curl -sLO https://github.com/babashka/babashka/releases/download/v1.3.191/babashka-1.3.191-macos-aarch64.tar.gz && tar xzvf babashka-1.3.191-macos-aarch64.tar.gz

cat bb target/cljconf.jar > cljconf

chmod +x cljconf
