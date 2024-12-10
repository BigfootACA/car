host=172.20.1.245
scp /home/bigfoot/code/linux-car/*.* root@$host:/opt/car/
ssh -t root@$host bash /opt/car/run-host.sh
