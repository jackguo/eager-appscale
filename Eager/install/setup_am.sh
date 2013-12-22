#!/bin/sh
echo "Setting Up WSO2 API Manager for EAGER"
echo "====================================="

apt-get install -y unzip
rm -rfv /root/APIManager
mkdir -p /root/temp
unzip $1 -d /root/temp
mv /root/temp/wso2am-* /root/APIManager
rm -rfv /root/temp

echo "Creating WSO2CARBON_DB database tables..."
mysql -uroot -p$2 WSO2CARBON_DB < /root/APIManager/dbscripts/mysql.sql

echo "Creating WSO2AM_DB database tables..."
mysql -uroot -p$2 WSO2AM_DB < /root/APIManager/dbscripts/apimgt/mysql.sql
