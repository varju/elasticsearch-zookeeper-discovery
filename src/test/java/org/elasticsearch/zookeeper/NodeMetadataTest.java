package org.elasticsearch.zookeeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class NodeMetadataTest {
  @Test
  public void toBytes() throws Exception {
    NodeMetadata metadata = new NodeMetadata("my.host", 9301, 9202);
    assertEquals("{\"hostname\":\"my.host\",\"transportPort\":9301,\"httpPort\":9202}", new String(metadata.toBytes(), "UTF-8"));
  }

  @Test
  public void equals() {
    NodeMetadata metadata = new NodeMetadata("host1", 9301, 9202);
    NodeMetadata same = new NodeMetadata("host1", 9301, 9202);
    NodeMetadata differentHostname = new NodeMetadata("host2", 9301, 9202);
    NodeMetadata differentTransportPort = new NodeMetadata("host1", 9302, 9202);
    NodeMetadata differentHttpPort = new NodeMetadata("host1", 9301, 9203);

    assertEquals(metadata, metadata);
    assertEquals(metadata, same);
    assertNotEquals(metadata, differentHostname);
    assertNotEquals(metadata, differentTransportPort);
    assertNotEquals(metadata, differentHttpPort);
  }

  @Test
  public void fromBytes() throws Exception {
    byte[] bytes = "{\"hostname\":\"my.host\",\"transportPort\":9301,\"httpPort\":9202}".getBytes("UTF-8");

    NodeMetadata metadata = NodeMetadata.fromBytes(bytes);
    assertEquals("my.host", metadata.getHostname());
    assertEquals(9301, metadata.getTransportPort());
    assertEquals(9202, metadata.getHttpPort());
  }
}
