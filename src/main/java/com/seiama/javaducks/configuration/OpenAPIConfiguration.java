package com.seiama.javaducks.configuration;

import com.seiama.javaducks.configuration.properties.AppConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenAPIConfiguration {
  @Bean
  OpenAPI openAPI(final AppConfiguration configuration) throws MalformedURLException {
    final OpenAPI api = new OpenAPI();
    api.info(
      new Info()
        .title(configuration.apiTitle())
        .version(configuration.apiVersion())
    );
    final URI apiBaseUrl = configuration.apiBaseUrl();
    if (apiBaseUrl != null) {
      api.servers(List.of(new Server().url(apiBaseUrl.toURL().toExternalForm())));
    }
    return api;
  }

  @Bean
  @SuppressWarnings("rawtypes") // nothing we can do, the API exposes it raw
  OpenApiCustomizer sortSchemasAlphabetically() {
    return openApi -> {
      final Map<String, Schema> schemas = openApi.getComponents().getSchemas();
      openApi.getComponents().setSchemas(new TreeMap<>(schemas));
    };
  }
}
