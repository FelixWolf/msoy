#!/usr/bin/env bash
docker build --network=host -t msoy-build .
docker create --name extract msoy-build
docker cp extract:/packages ./packages
docker rm extract