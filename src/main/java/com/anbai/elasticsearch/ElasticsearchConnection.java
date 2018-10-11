package com.anbai.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetSocketAddress;

/**
 * Created by yz on 2017/1/4.
 */
public class ElasticsearchConnection {

	/**
	 * 集群客户端连接对象
	 */
	private Client client;

	/**
	 * 集群节点配置
	 */
	private String nodes;

	/**
	 * 集群主机地址
	 */
	private String clusterHost = "127.0.0.1";

	/**
	 * 集群transport端口
	 */
	private int clusterPort = 9300;

	/**
	 * 集群名称
	 */
	private String clusterName = "elasticsearch";

	/**
	 * 是否嗅探集群
	 */
	private boolean transportSniff = true;

	/**
	 * 构建Elasticsearch Transport
	 *
	 * @param nodes          集群节点配置信息
	 * @param clusterName    集群名称
	 * @param transportSniff 是否嗅探集群
	 */
	public ElasticsearchConnection(String nodes, String clusterName, boolean transportSniff) {
		this.nodes = nodes;
		this.clusterName = clusterName;
		this.transportSniff = transportSniff;
	}

	/**
	 * 构建Elasticsearch Transport
	 *
	 * @param clusterHost    集群主机地址
	 * @param clusterPort    集群端口
	 * @param clusterName    集群名称
	 * @param transportSniff 是否嗅探集群
	 */
	public ElasticsearchConnection(String clusterHost, int clusterPort, String clusterName, boolean transportSniff) {
		this.clusterHost = clusterHost;
		this.clusterPort = clusterPort;
		this.clusterName = clusterName;
		this.transportSniff = transportSniff;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public String getNodes() {
		return nodes;
	}

	public void setNodes(String nodes) {
		this.nodes = nodes;
	}

	public String getClusterHost() {
		return clusterHost;
	}

	public void setClusterHost(String clusterHost) {
		this.clusterHost = clusterHost;
	}

	public int getClusterPort() {
		return clusterPort;
	}

	public void setClusterPort(int clusterPort) {
		this.clusterPort = clusterPort;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public boolean isTransportSniff() {
		return transportSniff;
	}

	public void setTransportSniff(boolean transportSniff) {
		this.transportSniff = transportSniff;
	}

	public void init() {
		Settings.Builder settingsBuilder = Settings.builder();
		settingsBuilder.put("cluster.name", clusterName);
		settingsBuilder.put("client.transport.sniff", transportSniff);
		PreBuiltTransportClient transportClient = new PreBuiltTransportClient(settingsBuilder.build());

		if (nodes != null && !"".equals(nodes)) {
			String[] strs = nodes.split(",");

			for (String str : strs) {
				String[] hostStr = str.split(":");
				String   host    = hostStr[0].trim();
				int      port    = hostStr.length > 0 ? Integer.parseInt(hostStr[1].trim()) : clusterPort;

				transportClient.addTransportAddress(
						new TransportAddress(
								new InetSocketAddress(host, port)
						)
				);
			}
		} else {
			transportClient.addTransportAddress(
					new TransportAddress(
							new InetSocketAddress(clusterHost, clusterPort)
					)
			);
		}

		this.client = transportClient;
	}

}
