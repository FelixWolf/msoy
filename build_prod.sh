#!/usr/bin/env bash
ant distcleanall
ant -Dflexsdk.dir=/home/felix/source/other/msoy/flex3 -Dmaven.repo.remote=https://repo1.maven.org/maven2 -Ddeployment=prod -Dmsoy.user=msoy -Dburl.user=msoy -Dmsoy.group=msoy package