package org.elasticsearch.zookeeper;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentFactory;

public class NodeMetadata {
  private final String hostname;
  private final int transportPort;
  private final int httpPort;

  public NodeMetadata(String hostname, int transportPort, int httpPort) {
    this.hostname = hostname;
    this.transportPort = transportPort;
    this.httpPort = httpPort;
  }

  public String getHostname() {
    return hostname;
  }

  public int getTransportPort() {
    return transportPort;
  }

  public int getHttpPort() {
    return httpPort;
  }

  public byte[] toBytes() throws IOException {
    String json = toJson();
    return json.getBytes("UTF-8");
  }

  private String toJson() {
    try {
      return XContentFactory.jsonBuilder()
        .startObject()
        .field("hostname", hostname)
        .field("transportPort", transportPort)
        .field("httpPort", httpPort)
        .endObject()
        .string();
    } catch (IOException e) {
      throw new RuntimeException("error creating json", e);
    }
  }

  public static NodeMetadata fromBytes(byte[] bytes) throws IOException {
    Map<String, Object> fields = XContentFactory.xContent(bytes).createParser(bytes).mapAndClose();
    return new NodeMetadata(
      (String) fields.get("hostname"),
      (Integer) fields.get("transportPort"),
      (Integer) fields.get("httpPort"));
  }

  @Override
  public String toString() {
    return toJson();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NodeMetadata that = (NodeMetadata) o;
    if (httpPort != that.httpPort) return false;
    if (transportPort != that.transportPort) return false;
    if (!hostname.equals(that.hostname)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = hostname.hashCode();
    result = 31 * result + transportPort;
    result = 31 * result + httpPort;
    return result;
  }
}
