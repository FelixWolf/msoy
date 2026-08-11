#!/bin/bash
chown msoy:msoy /export/msoy/pages/media
chown msoy:msoy /export/msoy/log

# Start all services
echo "Starting msoy-policy..."
#/export/msoy/etc/init.d/msoy-policy start
/export/msoy/bin/runpolicy &

echo "Starting msoy-server..."
/export/msoy/etc/init.d/msoy-server start

echo "Starting msoy-burl..."
/export/msoy/etc/init.d/msoy-burl start

# Keep the container alive
sleep 5
tail -f /export/msoy/log/*