#!/bin/bash
chown msoy:msoy /export/msoy/pages/media

# Start all services


echo "Starting msoy-server..."
/export/msoy/etc/init.d/msoy-server start

echo "Starting msoy-burl..."
/export/msoy/etc/init.d/msoy-burl start

echo "Starting msoy-policy..."
/export/msoy/etc/init.d/msoy-policy start

# Keep the container alive
tail -f /export/msoy/log/*