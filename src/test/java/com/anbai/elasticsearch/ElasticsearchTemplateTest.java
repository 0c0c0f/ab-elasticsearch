/*
 * Copyright yz 2017-12-08 Email:admin@javaweb.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anbai.elasticsearch;

import com.alibaba.fastjson.JSON;
import com.anbai.elasticsearch.bean.Documents;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;

public class ElasticsearchTemplateTest {

	/**
	 * 集群主机地址
	 */
	private String clusterHost = "127.0.0.1";

	/**
	 * 集群transport端口
	 */
	private int clusterPort = 9300;

	/**
	 * 是否嗅探集群
	 */
	private boolean transportSniff = true;

	/**
	 * 索引名称
	 */
	private String index = "web";

	/**
	 * type名称
	 */
	private String type = "documents";

	/**
	 * Elasticsearch 连接
	 */
	private ElasticsearchConnection connection;

	/**
	 * Elasticsearch 操作模板
	 */
	private ElasticsearchTemplate elasticsearchTemplate;

	/**
	 * 集群名称
	 */
	private String clusterName = "elasticsearch";

	private List<Documents> docs = new ArrayList();

	private static final Logger LOG = Logger.getLogger("info");

	@Before
	public void init() throws IOException {
		this.connection = new ElasticsearchConnection(clusterHost, clusterPort, clusterName, transportSniff);
		this.connection.init();
		this.elasticsearchTemplate = new ElasticsearchTemplate(this.connection);

		createIndex(10, 0);// 创建索引
		createMapping();// 创建映射关系
		initData();

		addIndex();

		try {
			Thread.sleep(1000);// 强制等待1秒钟
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 初始化数据
	 */
	private void initData() {
		for (int i = 0; i < 100; i++) {
			Documents doc = new Documents();
			doc.setUrl("http://www.baidu0" + i + 1 + ".com/");
			doc.setMethod(i > 70 ? "POST" : "GET");
			doc.setStatus(i < 60 ? 200 : 404);
			doc.setClientIp("192.168.1." + (i + 1));

			docs.add(doc);
		}
	}

	/**
	 * 创建 WebSearchMapping
	 *
	 * @return
	 * @throws IOException
	 */
	public boolean createMapping() throws IOException {
		XContentBuilder mapping = jsonBuilder().
				startObject().
				startObject(this.type).field("dynamic", true).
				startObject("_all").field("enabled", false).endObject().
				startObject("_source").field("enabled", true).endObject().
				startObject("properties").
				startObject("url").field("type", "text").endObject().
				startObject("method").field("type", "keyword").endObject().
				startObject("status").field("type", "integer").endObject().
				startObject("client_ip").field("type", "text").endObject().
				endObject().
				endObject().
				endObject();

		PutMappingResponse putMappingResponse = this.connection.getClient().admin().indices()
				.preparePutMapping(index)
				.setType(type)
				.setSource(mapping)
				.execute().actionGet();

		return putMappingResponse.isAcknowledged();
	}

	/**
	 * 创建索引
	 *
	 * @param shards   分片数
	 * @param replicas 副本数
	 * @return
	 */
	public boolean createIndex(int shards, int replicas) {
		CreateIndexRequestBuilder createIndexRequestBuilder = this.connection.
				getClient().admin().indices().prepareCreate(this.index);

		Map<String, Object> setting = new LinkedHashMap<String, Object>();
		setting.put("number_of_shards", shards);
		setting.put("number_of_replicas", replicas);
		createIndexRequestBuilder.setSettings(setting);
		CreateIndexResponse response = createIndexRequestBuilder.execute().actionGet();

		return response.isAcknowledged();
	}

	public void addIndex() {
		BulkRequestBuilder bulkRequest = this.connection.getClient().prepareBulk();

		docs.forEach(doc -> {
			try {
				String json = JSON.toJSONString(doc);

				// 如果doc类型已经是Map<String,Object>则不需要通过XContentParser解析,
				// 否则需将java对象序列化成json后再解析成Map才可以批量插入,老版本es无影响.
				XContentParser parser = XContentFactory.xContent(XContentType.JSON).
						createParser(NamedXContentRegistry.EMPTY,
								DeprecationHandler.THROW_UNSUPPORTED_OPERATION, json
						);

				bulkRequest.add(
						connection.getClient().prepareIndex(this.index, this.type).setSource(parser.map())
				);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		BulkResponse bulkResponse = bulkRequest.execute().actionGet();

		if (bulkResponse.hasFailures()) {
			throw new RuntimeException("批量导入数据异常!");
		} else {
			System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\t导入成功...");
		}
	}

	@Test
	public void queryForObject() {
		int pageNum  = 1;
		int pageSize = 10;

		SearchRequest searchRequest = elasticsearchTemplate.startQueryBuilder(index, type).
				setQuery(matchPhraseQuery("client_ip", "192.168.1.6")).
				setFrom(elasticsearchTemplate.convertElasticsearchPageNumber(pageNum, pageSize)).
				setSize(pageSize).
				request();

		Documents documents = elasticsearchTemplate.queryForObject(searchRequest, Documents.class);

		if (documents != null) {
			System.out.println(documents.getUrl());
			System.out.println(documents.getClientIp());
		}

	}

	@Test
	public void queryForPage() {
		int pageNum  = 1;
		int pageSize = 10;

		// 组合多条件查询
		BoolQueryBuilder bool = boolQuery();
		bool.must(matchPhraseQuery("status", 404));
		bool.must(matchPhraseQuery("method", "GET"));

		SearchRequest searchRequest = elasticsearchTemplate.startQueryBuilder(index, type).
				setQuery(bool).
				setFrom(elasticsearchTemplate.convertElasticsearchPageNumber(pageNum, pageSize)).
				setSize(pageSize).
				request();

		LOG.info("--------------------------------------------------------");

		Page<Documents> page = elasticsearchTemplate.queryForPage(searchRequest, Documents.class);

		System.out.println("总记录数:" + page.getTotalElements());
		List<Documents> documentsList = page.getContent();

		documentsList.forEach(document -> {
			System.out.println("URL:" + document.getUrl());
		});
	}

	@After
	public void clearIndex() {
		this.connection.getClient().admin().indices().delete(new DeleteIndexRequest(index));
	}

}
