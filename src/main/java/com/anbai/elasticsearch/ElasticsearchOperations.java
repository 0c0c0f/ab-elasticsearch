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
import org.elasticsearch.client.Client;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * ElasticsearchOperations
 */
public interface ElasticsearchOperations {

	/**
	 * 获取Elasticsearch 连接
	 *
	 * @return elasticsearch client
	 */
	Client getClient();

	/**
	 * 统计搜索命中的结果总数
	 *
	 * @param searchRequest
	 * @return
	 */
	long docCount(SearchRequest searchRequest);

	<T> T queryForObject(SearchRequest searchRequest, Class<T> clazz);

	<T> List<T> queryForList(SearchRequest searchRequest, Class<T> clazz);

	/**
	 * 执行elasticsearch 分页查询并返回分页结果 {@link Page}
	 *
	 * @param searchRequest
	 * @param clazz
	 * @return
	 */
	<T> Page<T> queryForPage(SearchRequest searchRequest, Class<T> clazz);

}
