package org.elasticsearch.zookeeper;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

/**
 * Stores a set of data in a set of ZooKeeper nodes for later retrieval. Data is only kept as long as the client that set the
 * value is connected.
 */
public class NodeSet implements Watcher, Iterable<Entry<String, NodeMetadata>> {
  private static final ESLogger logger = Loggers.getLogger(NodeSet.class);
  private final ZooKeeper        zoo;
  private final String        groupPath;
  private final Map<String, NodeMetadata>  nodeMap  = new ConcurrentHashMap<String, NodeMetadata>();

  public NodeSet(final ZKConnector zoo, final String groupPath) {
    this.zoo = zoo.getZk();
    this.groupPath = groupPath;
    try {
      getNodesFromZoo();
    } catch (Exception e) {
      logger.warn("Exception while processing watch", e);
    }
  }

  /**
   * React on an event fired by the ZooKeeper Cluster and update our known information.
   */
  @Override
  public void process(final WatchedEvent event) {
    if (event.getType() == EventType.NodeChildrenChanged) {
      try {
        getNodesFromZoo();
      } catch (Exception e) {
        logger.warn("Exception while processing watch", e);
      }

    }
    else if (event.getType() == EventType.NodeDataChanged) {
      final String path = event.getPath();
      final String[] compStrings = path.split("/");
      final String node = compStrings[compStrings.length - 1];

      try {
        remove(node);
        add(node);
      } catch (Exception e) {
        logger.warn("Exception while processing watch", e);
      }
    }
  }

  @Override
  public Iterator<Entry<String, NodeMetadata>> iterator() {
    return this.nodeMap.entrySet().iterator();
  }

  /**
   * Fetches data form ZooKeeper and checks what information needs to be updated.
   */
  private synchronized void getNodesFromZoo() throws KeeperException, InterruptedException, IOException {
    try {
      final Set<String> newState = new HashSet<String>(this.zoo.getChildren(this.groupPath, this));
      final Set<String> toDelete = new HashSet<String>(this.nodeMap.keySet());
      toDelete.removeAll(newState);

      final Set<String> toAdd = new HashSet<String>(newState);
      newState.removeAll(this.nodeMap.keySet());

      for (final String node : toDelete) {
        remove(node);
      }
      for (final String node : toAdd) {
        add(node);
      }
    } catch (KeeperException.NoNodeException e) {
      throw new RuntimeException("Group does not exist: " + this.groupPath, e);
    }
  }

  private void add(final String node) throws KeeperException, InterruptedException, IOException {
    final byte[] data = this.zoo.getData(this.groupPath + "/" + node, this, null);
    this.nodeMap.put(node, NodeMetadata.fromBytes(data));
  }

  private void remove(final String node) {
    this.nodeMap.remove(node);
  }
}
