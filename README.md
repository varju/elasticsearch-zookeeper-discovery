*Forked from https://github.com/mallocator/Elasticsearch-Zookeeper-Discovery*

Elasticsearch-Zookeeper-Discovery
=================================

A discovery module for Elasticsearch that allows nodes to find each other via communication over a Zookeeper cluster. Of course you need a running ZK server to get this working.

Requires Elasticsearch 1.4 or higher

Building
--------

To build the plugin you need to have maven installed. With that in mind simply check out the project and run `mvn package` in the project directory. The plugin should then be available under target/release as a .zip file.

Installation
------------

Just copy the .zip file on the elasticsearch server should be using the plugin and run the "plugin" script coming with elasticsearch in the bin folder.

An Example how one would call the plugin script:

```
/my/elasticsearch/bin/plugin -u file:///path/to/plugin/zookeeper-discovery.zip -i zookeeper-discovery
```

The plugin needs to be installed on all nodes of the ES cluster.

Configuration
-------------

In elasticsearch.yml:

```
discovery.zen.ping.multicast.enabled: false
discovery.type: zk
cloud.zk:
  enabled: true
  hosts:
  - zookeeper1:2181
  - zookeeper2:2181
  path: /elasticsearch
  http_port: 9200
```

Optional properties:

- `cloud.zk.path` - the zookeeper parent node containing the list of hosts; must already exist. Default: `/elasticsearch`
- `cloud.zk.http_port` - the externally visible HTTP port.  Default: 9200
