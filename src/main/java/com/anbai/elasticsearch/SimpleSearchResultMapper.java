package com.anbai.elasticsearch;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单的elasticsearch查询结果映射类
 *
 * @author yz
 */
public class SimpleSearchResultMapper {

	private DefaultEntityMapper entityMapper;

	public SimpleSearchResultMapper() {
		this.entityMapper = new DefaultEntityMapper();
	}

	public <T> Page<T> mapResults(SearchResponse response, Class<T> clazz, int pageNum, int pageSize) {
		TotalHits totalHits = response.getHits().getTotalHits();
		List<T>   results   = new ArrayList<T>();

		setSearchHit(response, clazz, results);

		return new PageImpl<T>(results, PageRequest.of(pageNum, pageSize), totalHits.value);
	}

	private <T> void setSearchHit(SearchResponse response, Class<T> clazz, List<T> results) {
		for (SearchHit hit : response.getHits()) {
			if (hit != null) {
				if (StringUtils.isNotBlank(hit.getSourceAsString())) {
					results.add(mapEntity(hit.getSourceAsString(), clazz));
				}
			}
		}
	}

	private <T> T mapEntity(String sourceAsString, Class<T> clazz) {
		try {
			return entityMapper.mapToObject(sourceAsString, clazz);
		} catch (Exception e) {
			throw new ElasticsearchEntityMapperException(e.toString());
		}
	}

}