# ab-elasticsearch

ab-elasticsearch是一个简化版的elasticsearch对象查询库,只提供了基本的es对象查询方法。在之前一直使用[Spring-Data-Elasticsearch](https://github.com/spring-projects/spring-data-elasticsearch)项目，但是它的更新速度实在是太慢了，而且无可参考文档、几乎每个新版本代码变动太大让人无法忍受。

在这里我只是封装了基本的Spring 和 elasticsearch的集成以及只有基本查询功能的ElasticsearchTemplate。当前版本已支持与Spring Boot共同配置。

## 在Maven项目中使用ab-elasticsearch

`ab-elasticsearch`版本号跟elasticsearch发布一致，目前已支持最新的elasticsearch7.2.0，在pom.xml添加ab-elasticsearch依赖即可。

```xml
<dependency>
    <groupId>com.anbai</groupId>
    <artifactId>ab-elasticsearch</artifactId>
    <version>7.2.0</version>
</dependency>
```

## ab-elasticsearch 与 Spring 集成

**添加如下pom.xml依赖**

```xml
<dependency>
    <groupId>org.elasticsearch</groupId>
    <artifactId>elasticsearch</artifactId>
    <version>7.2.0</version>
</dependency>

<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>transport</artifactId>
    <version>7.2.0</version>
</dependency>

<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-jpa</artifactId>
    <version>1.11.20.RELEASE</version>
</dependency>
```
需根据实际情况选择对应的依赖版本号。

**在Spring中配置elasticsearch集群连接和ElasticsearchTemplate**

```xml
<!-- 加载 elasticsearch 连接 -->
<bean id="elasticsearchConnection" class="com.anbai.elasticsearch.ElasticsearchConnection"
      init-method="init">
    <property name="clusterName" value="elasticsearch"/>
    <property name="clusterHost" value="127.0.0.1"/>
    <property name="clusterPort" value="9300"/>
    <property name="transportSniff" value="true"/>
</bean>

<!-- ElasticsearchTemplate 查询模板 -->
<bean id="elasticsearchTemplate" class="com.anbai.elasticsearch.ElasticsearchTemplate">
    <constructor-arg name="elasticsearchConnection" ref="elasticsearchConnection"/>
</bean>
```
`clusterName`填写集群名称,`clusterHost`填写集群主机地址(集群中的任意主机地址),`clusterPort`填写集群通信端口(注意是socket端口),`transportSniff`是否自动探测集群节点。


**在SpringBoot中配置**

在application.properties中添加:

```properties
# Elasticsearch 配置
#
elasticsearch.clusterName=elasticsearch
elasticsearch.clusterHost=127.0.0.1
elasticsearch.clusterPort=9300
elasticsearch.transportSniff=true
```

然后新建`ElasticsearchConfig.java`

```java
import com.anbai.elasticsearch.ElasticsearchConnection;
import com.anbai.elasticsearch.ElasticsearchTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ElasticsearchConfig {

	@Autowired
	private Environment environment;

	@Bean(value = "elasticsearchConnection", initMethod = "init")
	public ElasticsearchConnection elasticsearchConnection() {
		RelaxedPropertyResolver config         = new RelaxedPropertyResolver(environment, "elasticsearch.");
		String                  clusterName    = config.getProperty("clusterName");
		Boolean                 transportSniff = config.getProperty("transportSniff", Boolean.class);
		String                  clusterHost    = config.getProperty("clusterHost");
		int                     clusterPort    = Integer.parseInt(config.getProperty("clusterPort"));

		return new ElasticsearchConnection(clusterHost, clusterPort, clusterName, transportSniff);
	}

	@Bean("elasticsearchTemplate")
	public ElasticsearchTemplate elasticsearchTemplate(ElasticsearchConnection elasticsearchConnection) {
		return new ElasticsearchTemplate(elasticsearchConnection);
	}

}
```

**使用ElasticsearchTemplate做基本的查询**

```java
@Resource
private ElasticsearchTemplate elasticsearchTemplate;

public Page<Documents> search(int pageNum, int pageSize) {
	SearchRequest searchRequest = elasticsearchTemplate.startQueryBuilder(INDEX, TYPE).
			setQuery(matchAllQuery()).
			setFrom(elasticsearchTemplate.convertElasticsearchPageNumber(pageNum, pageSize)).
			setSize(pageSize).
			request();

	return elasticsearchTemplate.queryForPage(searchRequest, Documents.class);
}
```

Page对象不是spring-data-elasticsearch中的分页对象，不要搞混了。

**实体层映射**

```java
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class Documents {

	private String id;
	private String domain;

	@Field(type = FieldType.Nested)
	@JsonProperty("header_info")
	private Map<String, Object> headerInfo;
	
	@Field(type = Date, format = DateFormat.custom, pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date mtime;
	
	......
```

当实体类中的成员变量和es中的名称不一致时，可以用jackson的注解绑定两者。

## 版本更新

1. 本次更新升级了elasticsearch(6.1.1)和spring-data-elasticsearch(3.0.2.RELEASE)版本为最新版本，移除了原来对spring-data-elasticsearch项目的依赖。
2. 升级了ElasticSearch版本(6.4.2) 2018-10-11
3. 升级elasticsearch(7.0.0) 2019-04-30
4. 升级elasticsearch(7.2.0) 2019-07-04

## Notice

1. 这个项目由我的另一个[javaweb-elasticsearch](https://github.com/javasec/javaweb-elasticsearch)项目更名而来,以后维护的可能主要是此项目.
