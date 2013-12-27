#!/bin/sh
echo "Setting Up WSO2 API Manager for EAGER"
echo "====================================="

apt-get install -y unzip
rm -rfv /root/APIManager
mkdir -p /root/temp
unzip $1 -d /root/temp
mv /root/temp/wso2am-* /root/APIManager
rm -rfv /root/temp

echo "Creating databases and user accounts."

echo "Creating database WSO2CARBON_DB for user $3"
mysql -uroot -p$2 -e <<EOSQL "DROP DATABASE IF EXISTS WSO2CARBON_DB; CREATE DATABASE WSO2CARBON_DB; GRANT ALL PRIVILEGES ON WSO2CARBON_DB.* TO $3@localhost IDENTIFIED BY '$4'"
EOSQL
RC=$?
if [ $RC != 0 ]; then
    exit $RC
fi

echo "Creating WSO2CARBON_DB database tables..."
mysql -uroot -p$2 WSO2CARBON_DB < /root/APIManager/dbscripts/mysql.sql

echo "Creating database WSO2AM_DB for user $3"
mysql -uroot -p$2 -e <<EOSQL "DROP DATABASE IF EXISTS WSO2AM_DB; CREATE DATABASE WSO2AM_DB; GRANT ALL PRIVILEGES ON WSO2AM_DB.* TO $3@localhost IDENTIFIED BY '$4'"
EOSQL
RC=$?
if [ $RC != 0 ]; then
    exit $RC
fi

echo "Creating WSO2AM_DB database tables..."
mysql -uroot -p$2 WSO2AM_DB < /root/APIManager/dbscripts/apimgt/mysql.sql
mysql -uroot -p$2 WSO2AM_DB < /root/appscale/Eager/install/eager.sql
