#!/bin/bash
set -ex

docker build \
    -t muzei-bingimageoftheday \
    .

docker run --rm \
    -v "$PWD":/home/gradle/ \
    -w /home/gradle \
    muzei-bingimageoftheday \
    $(mkdir Key) && $(touch Key/keystore.properties) && $(gradle build)