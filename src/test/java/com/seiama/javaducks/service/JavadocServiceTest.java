/*
 * This file is part of javaducks, licensed under the MIT License.
 *
 * Copyright (c) 2023-2024 Seiama
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.seiama.javaducks.service;

import com.seiama.javaducks.configuration.properties.AppConfiguration;
import com.seiama.javaducks.util.crypto.HashAlgorithm;
import com.seiama.javaducks.util.maven.MavenHashType;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class JavadocServiceTest {

  @Test
  void refreshAllResolvesPinnedReleaseToJavadocJar() throws Exception {
    final byte[] jarBytes = "jar".getBytes(StandardCharsets.UTF_8);
    final String sha512 = HashAlgorithm.SHA512.hash(jarBytes).toString();
    final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/io/papermc/paper/paper-api/26.1.1.build.28-alpha/paper-api-26.1.1.build.28-alpha-javadoc.jar", exchange -> this.respond(exchange, jarBytes));
    server.createContext("/io/papermc/paper/paper-api/26.1.1.build.28-alpha/paper-api-26.1.1.build.28-alpha-javadoc.jar.sha512", exchange -> this.respond(exchange, sha512));
    server.start();
    try {
      final Path storage = Files.createTempDirectory("javaducks-pinned-release");
      final AppConfiguration configuration = new AppConfiguration(
        java.net.URI.create("https://example.com"),
        java.net.URI.create("https://example.com"),
        storage,
        List.of(new AppConfiguration.EndpointConfiguration(
          "paper",
          List.of(new AppConfiguration.EndpointConfiguration.Version(
            "26.1.1",
            null,
            java.net.URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"),
            "io.papermc.paper",
            "paper-api",
            "26.1.1.build.28-alpha",
            AppConfiguration.EndpointConfiguration.Version.Type.MAVEN
          ))
        )),
        List.of(MavenHashType.SHA512)
      );

      new JavadocService(configuration, RestClient.create()).refreshAll();

      assertThat(Files.readAllBytes(storage.resolve("paper").resolve("26.1.1.jar"))).isEqualTo(jarBytes);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void refreshAllResolvesChangingReleaseToLatestMatchingBuild() throws Exception {
    final byte[] jarBytes = "jar".getBytes(StandardCharsets.UTF_8);
    final String sha512 = HashAlgorithm.SHA512.hash(jarBytes).toString();
    final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/io/papermc/paper/paper-api/maven-metadata.xml", exchange -> this.respond(exchange, metadata()));
    server.createContext("/io/papermc/paper/paper-api/26.1.1.build.28-alpha/paper-api-26.1.1.build.28-alpha-javadoc.jar", exchange -> this.respond(exchange, jarBytes));
    server.createContext("/io/papermc/paper/paper-api/26.1.1.build.28-alpha/paper-api-26.1.1.build.28-alpha-javadoc.jar.sha512", exchange -> this.respond(exchange, sha512));
    server.start();
    try {
      final Path storage = Files.createTempDirectory("javaducks-release-selector");
      final AppConfiguration configuration = new AppConfiguration(
        java.net.URI.create("https://example.com"),
        java.net.URI.create("https://example.com"),
        storage,
        List.of(new AppConfiguration.EndpointConfiguration(
          "paper",
          List.of(new AppConfiguration.EndpointConfiguration.Version(
            "26.1.1",
            null,
            java.net.URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"),
            "io.papermc.paper",
            "paper-api",
            "26.1.1.build.+",
            AppConfiguration.EndpointConfiguration.Version.Type.MAVEN
          ))
        )),
        List.of(MavenHashType.SHA512)
      );

      new JavadocService(configuration, RestClient.create()).refreshAll();

      assertThat(Files.readAllBytes(storage.resolve("paper").resolve("26.1.1.jar"))).isEqualTo(jarBytes);
    } finally {
      server.stop(0);
    }
  }

  private static String metadata() {
    return """
      <metadata>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <versioning>
          <versions>
            <version>26.1.0.build.5-alpha</version>
            <version>26.1.1.build.27-alpha</version>
            <version>26.1.1.build.28-alpha</version>
          </versions>
        </versioning>
      </metadata>
      """;
  }

  private void respond(final HttpExchange exchange, final String body) throws IOException {
    this.respond(exchange, body.getBytes(StandardCharsets.UTF_8));
  }

  private void respond(final HttpExchange exchange, final byte[] body) throws IOException {
    exchange.sendResponseHeaders(200, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }
}
