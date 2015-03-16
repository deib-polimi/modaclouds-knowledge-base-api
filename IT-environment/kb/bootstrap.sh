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

apt-get update
apt-get install -y tar openjdk-7-jre

if [ -d /opt/fuseki ];
	then rm -r /opt/fuseki
fi

mkdir -p /opt/fuseki
cd /opt/fuseki

echo "Downloading fuseki..."
wget --quiet -O jena-fuseki-1.1.1-distribution.tar.gz http://archive.apache.org/dist/jena/binaries/jena-fuseki-1.1.1-distribution.tar.gz
tar -xvzf jena-fuseki-1.1.1-distribution.tar.gz -C .
chmod +x jena-fuseki-1.1.1/fuseki-server