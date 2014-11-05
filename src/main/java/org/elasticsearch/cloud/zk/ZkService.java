package org.elasticsearch.cloud.zk;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.component.LifecycleListener;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.zookeeper.*;

/**
 * This service establishes the actual connection to the ZooKeeper and finds the other nodes of the cluster.
 */
public class ZkService extends AbstractLifecycleComponent<ZkService> {
  private final ZKConnector    zooConnector;
  private final NodeSet<String>  nodes;
  private final String      zkPath;
  private String          nodeAddress;
  private final TransportService transportService;
  private NodeSetMember      groupMember;

  @Inject
  public ZkService(final Settings settings, final SettingsFilter settingsFilter, TransportService transportService) {
    super(settings);
    settingsFilter.addFilter(new ZkSettingsFilter());

    final StringBuilder hosts = new StringBuilder();
    for (String host : settings.getAsArray("cloud.zk.hosts")) {
      hosts.append(",").append(host);
    }
    if (hosts.length() > 1) {
      this.zooConnector = new ZKConnector(hosts.substring(1));
    }
    else {
      this.logger.error("ZooKeeper Service initialisation failed (hosts: {})", settings.get("cloud.zk.hosts"));
      throw new RuntimeException("ZooKeeper Service initialisation has failed - no hosts were supplied");
    }

    this.zkPath = settings.get("cloud.zk.path", "/elasticsearch");
    this.nodes = new NodeSet<String>(this.zooConnector, this.zkPath);

    this.transportService = transportService;
    transportService.addLifecycleListener(new LifecycleListener() {
      @Override
      public void afterStart() {
        try {
          String myIpAddress = ZkService.this.transportService.boundAddress().publishAddress().toString();
          ZkService.this.nodeAddress = myIpAddress.replaceFirst("^inet\\[/", "").replaceFirst("\\]$", "");
          registerNode();
        } catch (Exception e) {
          logger.info("Failed to register node with zookeeper", e);
        }
      }

      @Override
      public void beforeStop() {
        unregisterNode();
      }
    });
  }

  public String getNodeAddress() {
    return this.nodeAddress;
  }

  public NodeSet<String> getNodes() {
    return this.nodes;
  }

  @Override
  protected void doStart() throws ElasticsearchException {
  }

  @Override
  protected void doStop() throws ElasticsearchException {
  }

  @Override
  protected void doClose() throws ElasticsearchException {
  }

  /**
   * Registers a node at a specified ZooKeeper path, so that other nodes can find this node.
   */
  private void registerNode() {
    if (this.nodeAddress == null) {
      this.logger.warn("Can't register with ZooKeeper, as I don't know my own address yet");
      return;
    }
    if (this.groupMember != null) {
      this.logger.warn("Already registered with ZooKeeper, skipping registration");
      return;
    }
    this.groupMember = new NodeSetMember(this.zooConnector, this.zkPath, getZKNodeName(), this.nodeAddress);
    this.groupMember.registerNode();
    this.logger.info("Registered with ZooKeeper under node {} with address {}", getZKNodeName(), this.nodeAddress);
  }

  private void unregisterNode() {
    if (this.groupMember != null) {
      this.groupMember.unregisterNode();
    }
  }

  private String getZKNodeName() {
    return nodeName().replaceAll("[,|\\.| |']", "").trim();
  }
}
