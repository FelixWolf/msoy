#!/bin/bash
set -e

# Required overlay dirs
mkdir -p /overlay/upper /overlay/work /overlay/media

# Mount overlay with existing .deb-installed files as lowerdir
mount -t overlay overlay -o lowerdir=/export/msoy/pages/media,upperdir=/overlay/upper,workdir=/overlay/work /overlay/media

# Bind-mount it *over* the original path, so the app doesn't see a different location
mount --bind /overlay/media /export/msoy/pages/media

# Continue to main app
exec "$@"
