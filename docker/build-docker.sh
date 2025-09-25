#!/bin/bash
set -e

# Set source and destination directories
SRC_DIR="../dist/packages"
DST_DIR="docker-staging"

# Clean staging directory
mkdir -p "$DST_DIR"
rm -f "$DST_DIR"/*.dpkg

# List of base package names (prefixes)
for prefix in msoy-server msoy-server-code burl-server; do
    # Find the latest matching file
    latest=$(ls -1 "$SRC_DIR/${prefix}_"*.dpkg 2>/dev/null | sort | tail -n 1)
    if [[ -n "$latest" ]]; then
        echo "Copying latest $prefix package: $latest"
        cp "$latest" "$DST_DIR/"
    else
        echo "Warning: No package found for $prefix"
    fi
done

DOCKER_HOSTNAME=msoy0 docker-compose build
