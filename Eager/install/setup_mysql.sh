#!/bin/sh
echo "Setting Up MySQL for EAGER"
echo "=========================="
export DEBIAN_FRONTEND=noninteractive
if command -v mysql > /dev/null; then
    echo "MySQL already installed."
else
    echo "Installing MySQL..."
    apt-get -y install mysql-server
    echo "Waiting for MySQL server to start..."
    sleep 5
fi
echo "Setting password for MySQL user: $1."
mysql -uroot -e <<EOSQL "UPDATE mysql.user SET Password=PASSWORD('$2') WHERE User='$1'; FLUSH PRIVILEGES;"
EOSQL
echo "Done setting MySQL password."