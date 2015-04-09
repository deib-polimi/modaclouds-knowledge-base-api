#!/bin/sh
#
# Copyright 2014 deib-polimi
# Contact: deib-polimi <marco.miglierina@polimi.it>
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

pkill -f 'java.*fuseki-server'

if [ ! -d /opt/fuseki/ds ];
	then mkdir -p /opt/fuseki/ds
fi
rm -rf /opt/fuseki/ds/*
cd /opt/fuseki/jena-fuseki-*/
mkdir -p /vagrant/logs
echo "Starting kb..."
./fuseki-server --update --loc /opt/fuseki/ds /modaclouds/kb > /vagrant/logs/kb.log 2>&1 &