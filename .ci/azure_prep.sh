#!/bin/bash
mkdir Key || true
cp $1/keystore.properties Key/keystore.properties
cp $1/muzei-bing.keystore.jks Key/muzei-bing.keystore.jks
