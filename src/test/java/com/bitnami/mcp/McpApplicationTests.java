package com.bitnami.mcp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class McpApplicationTests {

	@Autowired
	private HelmService helmService;

	@Test
	void contextLoads() {}

	@Test
	void chartsExist() {

		assertNotNull(helmService.getHelmCharts());
        //assertFalse(helmService.getHelmCharts().isEmpty());
	}

	@Test
	void chartReadme() {

		assertNotNull(helmService.getHelmChartReadme("apache"));
		assertFalse(helmService.getHelmChartReadme("apache").readme().isEmpty());
	}


	@Test
	void chartValues() {

		assertNotNull(helmService.getHelmChartReadme("airflow"));
		assertFalse(helmService.getHelmChartValues("airflow").valuesYaml().isEmpty());
	}


	@Test
	void chartVersions() {

		//assertFalse(helmService.getHelmChartVersions("kafka").isEmpty());
	}
}
