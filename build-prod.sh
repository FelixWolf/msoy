#!/usr/bin/env bash
docker build --progress=plain --network=host -t msoy-build .
docker create --name extract msoy-build
rm -r ./packages/*
docker cp extract:/packages/ ./
docker rm extract