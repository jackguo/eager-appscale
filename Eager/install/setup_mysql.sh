#!/bin/sh
echo "Setting Up MySQL for EAGER"
echo "=========================="

if command -v mysql > /dev/null; then
    echo "MySQL already installed. Assuming the root password is correctly set."
else
    export DEBIAN_FRONTEND=noninteractive
    echo "Installing MySQL..."
    apt-get -y install mysql-server
    echo "Waiting for MySQL server to start..."
    sleep 5
    echo "Setting password for MySQL user: root."
    mysql -uroot -e <<EOSQL "UPDATE mysql.user SET Password=PASSWORD('$1') WHERE User='root'; FLUSH PRIVILEGES;"
EOSQL
    echo "Done setting MySQL root password."
fi

echo "Creating databases and user accounts."

mysql -uroot -p$1 -e <<EOSQL "DROP DATABASE IF EXISTS WSO2_CARBON_DB; CREATE DATABASE WSO2_CARBON_DB; GRANT ALL PRIVILEGES ON WSO2_CARBON_DB.* TO $2@localhost IDENTIFIED BY '$3'"
EOSQL
RC=$?
if [ $RC != 0 ]; then
    exit $RC
fi
echo "Created database WSO2_CARBON_DB for user $2"

mysql -uroot -p$1 -e <<EOSQL "DROP DATABASE IF EXISTS WSO2_AM_DB; CREATE DATABASE WSO2_AM_DB; GRANT ALL PRIVILEGES ON WSO2_AM_DB.* TO $2@localhost IDENTIFIED BY '$3'"
EOSQL
RC=$?
if [ $RC != 0 ]; then
    exit $RC
fi
echo "Created database WSO2_AM_DB for user $2"

echo "All done."
