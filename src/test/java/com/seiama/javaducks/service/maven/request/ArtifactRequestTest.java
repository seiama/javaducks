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
package com.seiama.javaducks.service.maven.request;

import com.seiama.javaducks.util.maven.MavenConstants;
import com.seiama.javaducks.util.maven.MavenHashType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactRequestTest {
  @Test
  void testToUrl() {
    final ArtifactRequest request = new ArtifactRequest(
      "io.netty",
      "netty-buffer",
      "4.1.106.Final",
      null,
      null,
      null,
      MavenConstants.EXTENSION_JAR,
      null,
      null
    );
    assertEquals("io/netty/netty-buffer/4.1.106.Final/netty-buffer-4.1.106.Final.jar", request.toUrl());

    final ArtifactRequest requestWithHash = request.withHash(MavenHashType.SHA256);
    assertEquals("io/netty/netty-buffer/4.1.106.Final/netty-buffer-4.1.106.Final.jar.sha256", requestWithHash.toUrl());

    final ArtifactRequest requestWithSnapshot = request.withVersion("4.1.106.Final-SNAPSHOT").withSnapshot("20240119095726", 5);
    assertEquals("io/netty/netty-buffer/4.1.106.Final-SNAPSHOT/netty-buffer-4.1.106.Final-20240119095726-5.jar", requestWithSnapshot.toUrl());
  }
}
