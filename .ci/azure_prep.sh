#!/bin/bash
mkdir Key || true
ls $1
cp $1/keystore.properties Key/keystore.properties
cp $1/muzei-bing.keystore.jks Key/muzei-bing.keystore.jks
