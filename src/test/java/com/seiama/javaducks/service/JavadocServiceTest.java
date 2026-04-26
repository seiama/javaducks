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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JavadocServiceTest {

  @Test
  void refreshAllResolvesPinnedReleaseToJavadocJar() throws Exception {
    final byte[] jarBytes = "jar".getBytes(StandardCharsets.UTF_8);
    final String sha512 = HashAlgorithm.SHA512.hash(jarBytes).toString();

    final RestClient.Builder builder = RestClient.builder();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    final RestClient restClient = builder.build();

    final String jarUri = "http://repo.test/io/papermc/paper/paper-api/26.1.1.build.28-alpha/paper-api-26.1.1.build.28-alpha-javadoc.jar";
    final String hashUri = jarUri + ".sha512";

    server.expect(requestTo(hashUri)).andRespond(withSuccess(sha512, MediaType.TEXT_PLAIN));
    server.expect(requestTo(jarUri)).andRespond(withSuccess(jarBytes, MediaType.APPLICATION_OCTET_STREAM));

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
          java.net.URI.create("http://repo.test/"),
          "io.papermc.paper",
          "paper-api",
          "26.1.1.build.28-alpha",
          AppConfiguration.EndpointConfiguration.Version.Type.MAVEN
        ))
      )),
      List.of(MavenHashType.SHA512)
    );

    new JavadocService(configuration, restClient).refreshAll();

    assertThat(Files.readAllBytes(storage.resolve("paper").resolve("26.1.1.jar"))).isEqualTo(jarBytes);

    server.verify();
  }

  @Test
  void refreshAllResolvesChangingReleaseToLatestMatchingBuild() throws Exception {
    final byte[] jarBytes = "jar".getBytes(StandardCharsets.UTF_8);
    final String sha512 = HashAlgorithm.SHA512.hash(jarBytes).toString();

    final RestClient.Builder builder = RestClient.builder();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    final RestClient restClient = builder.build();

    final String metadataUri = "http://repo.test/io/papermc/paper/paper-api/maven-metadata.xml";
    final String jarUri = "http://repo.test/io/papermc/paper/paper-api/26.1.1.build.28-alpha/paper-api-26.1.1.build.28-alpha-javadoc.jar";
    final String hashUri = jarUri + ".sha512";

    server.expect(requestTo(metadataUri)).andRespond(withSuccess(metadata(), MediaType.TEXT_PLAIN));
    server.expect(requestTo(hashUri)).andRespond(withSuccess(sha512, MediaType.TEXT_PLAIN));
    server.expect(requestTo(jarUri)).andRespond(withSuccess(jarBytes, MediaType.APPLICATION_OCTET_STREAM));

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
          java.net.URI.create("http://repo.test/"),
          "io.papermc.paper",
          "paper-api",
          "26.1.1.build.+",
          AppConfiguration.EndpointConfiguration.Version.Type.MAVEN
        ))
      )),
      List.of(MavenHashType.SHA512)
    );

    new JavadocService(configuration, restClient).refreshAll();

    assertThat(Files.readAllBytes(storage.resolve("paper").resolve("26.1.1.jar"))).isEqualTo(jarBytes);

    server.verify();
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
}
