/*
 * Copyright 2013-2016 the original author or authors.
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

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * ElasticsearchTemplate 查询模板,提供基本的对象查询和结果映射
 * 因为Spring Data Elasticsearch 项目更新速度太慢了,复制了ElasticsearchTemplate部分功能
 *
 * @author yz
 */
public class ElasticsearchTemplate implements ElasticsearchOperations {

	private final Client client;

	private final ElasticsearchConnection elasticsearchConnection;

	public ElasticsearchTemplate(ElasticsearchConnection elasticsearchConnection) {
		this.elasticsearchConnection = elasticsearchConnection;
		this.client = elasticsearchConnection.getClient();
	}

	@Override
	public Client getClient() {
		return this.elasticsearchConnection.getClient();
	}

	/**
	 * 统计搜索命中的结果总数
	 *
	 * @param searchRequest
	 * @return
	 */
	@Override
	public long docCount(SearchRequest searchRequest) {
		return client.search(searchRequest).actionGet().getHits().getTotalHits().value;
	}

	/**
	 * 查询单个对象
	 *
	 * @param searchRequest
	 * @param clazz
	 * @return
	 * @throws IncorrectResultSizeDataAccessException
	 */
	@Override
	public <T> T queryForObject(SearchRequest searchRequest, Class<T> clazz) {
		List<T> list = queryForList(searchRequest, clazz);

		if (list.size() > 1) {
			new IncorrectResultSizeDataAccessException(list.size()).printStackTrace();
		} else if (list.size() == 1) {
			return list.get(0);
		}

		return null;
	}

	/**
	 * 查询一个结果集数组
	 *
	 * @param searchRequest
	 * @param clazz
	 * @return
	 */
	@Override
	public <T> List<T> queryForList(SearchRequest searchRequest, Class<T> clazz) {
		Page<T> pages = queryForPage(searchRequest, clazz);

		if (pages != null && pages.getTotalElements() > 0) {
			return pages.getContent();
		}
		return new ArrayList<T>();
	}

	/**
	 * 分页查询
	 *
	 * @param searchRequest
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	@Override
	public <T> Page<T> queryForPage(SearchRequest searchRequest, Class<T> clazz) {
		SearchResponse response = client.search(searchRequest).actionGet();
		int            from     = searchRequest.source().from();
		int            pageSize = searchRequest.source().size();
		int            pageNum  = convertPageNumber(from, pageSize);

		return new SimpleSearchResultMapper().mapResults(response, clazz, pageNum, pageSize);
	}

	public SearchRequestBuilder startQueryBuilder(String[] index, String[] type) {
		return this.getClient().prepareSearch(index).setTypes(type);
	}

	public SearchRequestBuilder startQueryBuilder(String index, String type) {
		return startQueryBuilder(new String[]{index}, new String[]{type});
	}

	public int convertElasticsearchPageNumber(int pageNum, int pageSize) {
		return (pageNum - 1) * pageSize;
	}

	public int convertPageNumber(int from, int size) {
		return from / size + 1;
	}

}
