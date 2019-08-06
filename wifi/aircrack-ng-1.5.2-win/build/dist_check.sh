#!/usr/bin/env bash

set -eufx

#
# Bail on OS X for testing this functionality.
#
if [ "$TRAVIS_OS_NAME" == "osx" ]; then
    exit 0
fi

find . -name .deps -o -name '*.la' -o -name .libs -o -name Makefile -print0 | xargs -0 rm -vfr
if [ "$TRAVIS_OS_NAME" == "osx" ]; then
    ./autogen.sh --with-openssl=/usr/local/Cellar/openssl/1.0.2l --with-experimental --with-ext-scripts
else
    ./autogen.sh
fi
make
make dist

V=$(./evalrev)
BN="aircrack-ng-$V"

[ -d dist_build ] && rm -fr dist_build
mkdir dist_build
cd dist_build

tar xzf ../$BN.tar.gz
cd "$BN"
if [ "$TRAVIS_OS_NAME" == "osx" ]; then ./configure --with-openssl=/usr/local/Cellar/openssl/1.0.2l --with-experimental --with-ext-scripts; else ./configure --with-experimental --with-ext-scripts; fi
make
make check || { find test -name 'test-suite.log' -exec cat {} ';' && exit 1; }
make DESTDIR=/tmp/ac install

exit 0
