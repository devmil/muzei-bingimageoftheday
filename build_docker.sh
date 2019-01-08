#!/bin/bash
set -ex

docker build \
    -t muzei-bingimageoftheday \
    .

docker run --rm \
    -v "$PWD":/home/app \
    -w /home/app \
    muzei-bingimageoftheday \
    bash .ci/build.sh