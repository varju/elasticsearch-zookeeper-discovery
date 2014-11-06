package org.elasticsearch.discovery.zk;

import java.util.List;
import java.util.Map.Entry;

import org.elasticsearch.Version;
import org.elasticsearch.cloud.zk.ZkService;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.discovery.zen.ping.unicast.UnicastHostsProvider;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.zookeeper.NodeMetadata;

/**
 * Is used to register this node and create a list of available nodes in the cluster.
 */
public class ZkUnicastHostsProvider extends AbstractComponent implements UnicastHostsProvider {
  private final TransportService  transportService;
  private final ZkService      zkService;
  private final Version version;

  public ZkUnicastHostsProvider(final Settings settings, final TransportService transportService, final ZkService zkService, final Version version) {
    super(settings);
    this.version = version;
    this.zkService = zkService;
    this.transportService = transportService;
  }

  @Override
  public List<DiscoveryNode> buildDynamicNodes() {
    this.logger.info("Building list of dynamic discovery nodes from ZooKeeper");
    final NodeMetadata myAddress = this.zkService.getNodeMetadata();

    final List<DiscoveryNode> discoNodes = Lists.newArrayList();
    int clientCount = 0;
    for (Entry<String, NodeMetadata> entry : this.zkService.getNodes()) {
      NodeMetadata entryValue = entry.getValue();
      if (entryValue.equals(myAddress)) {
        this.logger.debug("Skipping myself: node \"{}\" with value {}", entry.getKey(), entryValue);
        continue;
      }
      clientCount++;
      try {
        int i = 0;
        String discoveryAddress = entryValue.getHostname() + ":" + entryValue.getTransportPort();
        for (TransportAddress address : this.transportService.addressesFromString(discoveryAddress)) {
          this.logger.debug("Found node \"{}\" with address {}", entry.getKey(), address);
          discoNodes.add(new DiscoveryNode("#cloud-" + entry.getKey() + "-" + i++, address, version));
        }
      } catch (Exception e) {
        this.logger.warn("Can't add address {} as valid DiscoveryNode", entryValue);
      }
    }
    this.logger.info("Found {} other nodes via ZooKeeper", clientCount);

    return discoNodes;
  }
}
