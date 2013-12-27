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

