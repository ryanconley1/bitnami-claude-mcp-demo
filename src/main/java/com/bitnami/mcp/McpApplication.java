package com.bitnami.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.List;

@SpringBootApplication
public class McpApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpApplication.class, args);
	}

	@Bean
	public List<ToolCallback> bitnamiTools(HelmService helmService) {
		return List.of(ToolCallbacks.from(helmService));
	}

	//TODO: Claude is not doing anything particularly useful with this so I'm sure I'm doing something wrong
	@Bean
	public List<McpServerFeatures.SyncResourceRegistration> myResources(HelmService helmService) {

		var resourceRegistration = new McpServerFeatures.SyncResourceRegistration(
				new McpSchema.Resource(
						"helm://{chart}/readme",
						"Helm chart README files for the different applications.",
						"Contains information on how to use the different Helm chart applications.",
						"text/markdown",
						null
						),
				(request) -> {
					final String uri = request.uri();
					final String chart = uri.substring("helm://".length())
							.split("/")[0];

					return new McpSchema.ReadResourceResult(
							List.of(new McpSchema.TextResourceContents(
									String.format("helm://%s/readme", chart),
									"text/markdown",
									helmService.readReadme(chart)
								)));
				}
		) ;
		return List.of(resourceRegistration);
	}
}
