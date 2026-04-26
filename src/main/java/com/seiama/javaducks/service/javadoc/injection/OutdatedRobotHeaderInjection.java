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
package com.seiama.javaducks.service.javadoc.injection;

import com.seiama.javaducks.configuration.properties.AppConfiguration;
import com.seiama.javaducks.service.javadoc.JavadocKey;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class OutdatedRobotHeaderInjection implements Injection {
  private final AppConfiguration configuration;

  public OutdatedRobotHeaderInjection(final AppConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public boolean canInject(final Path file, final JavadocKey key) {
    return file.toString().endsWith(HTML) && !this.isLatestVersion(key).isLatest();
  }

  @Override
  public String inject(final String line, final Path file, final JavadocKey key) {
    if (line.contains("</head>")) {
      return "<link rel=\"canonical\" href=\"%s\">\n%s".formatted(
        this.configuration.hostName().resolve(key.project() + "/" + this.isLatestVersion(key).latest() + "/" + file),
        line
      );
    }
    return line;
  }

  private LatestPair isLatestVersion(final JavadocKey key) {
    return this.configuration.endpoints().stream()
      .filter(e -> e.name().equals(key.project()))
      .findFirst()
      .map(e -> {
        final List<AppConfiguration.EndpointConfiguration.Version> list = e.versions().stream().filter(version -> version.type() == AppConfiguration.EndpointConfiguration.Version.Type.RELEASE).toList();
        final String latestVersion = list.get(list.size() - 1).name();
        final boolean isLatest = latestVersion.equals(key.version());
        return new LatestPair(isLatest, latestVersion);
      })
      .orElse(new LatestPair(false, null));
  }

  private record LatestPair(boolean isLatest, @Nullable String latest) {}

}
