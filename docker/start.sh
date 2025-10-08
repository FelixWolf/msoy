#!/bin/bash
chown msoy:msoy /export/msoy/pages/media

# Start all services
echo "Starting msoy-policy..."
#/export/msoy/etc/init.d/msoy-policy start
/export/msoy/bin/runpolicy &

echo "Starting msoy-server..."
touch /export/msoy/log/world-server.log
/export/msoy/etc/init.d/msoy-server start

echo "Starting msoy-burl..."
touch /export/msoy/log/world-server.log
/export/msoy/etc/init.d/msoy-burl start

# Keep the container alive
tail -f /export/msoy/log/*