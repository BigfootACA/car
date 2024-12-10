time="$(date '+%Y-%m-%d %H:%M:%S')"
systemctl restart car-server
sleep 3
journalctl -b -u car-server -S "$time" --no-pager
