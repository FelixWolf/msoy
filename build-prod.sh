#!/usr/bin/env bash
docker build --network=host -t msoy-build . --no-cache
docker create --name extract msoy-build
rm -r ./packages/*
docker cp extract:/packages/ ./
docker rm extract