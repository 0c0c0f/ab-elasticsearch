package com.anbai.elasticsearch;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;


/**
 * DocumentMapper using jackson
 *
 * @author Artur Konczak
 * @author Petar Tahchiev
 */
public class DefaultEntityMapper {

	private ObjectMapper objectMapper;

	public DefaultEntityMapper() {
		objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	}

	public String mapToString(Object object) throws IOException {
		return objectMapper.writeValueAsString(object);
	}

	public <T> T mapToObject(String source, Class<T> clazz) throws IOException {
		return objectMapper.readValue(source, clazz);
	}

}