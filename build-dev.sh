#!/usr/bin/env bash
docker build --network=host --build-arg DEPLOYMENT=test --build-arg DEV_DEPLOYMENT=true -t msoy-test .
docker create --name extract msoy-test
rm ./packages/*
docker cp extract:/packages ./packages
docker rm extract