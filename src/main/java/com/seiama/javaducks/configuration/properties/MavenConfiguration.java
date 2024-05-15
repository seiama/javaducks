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
package com.seiama.javaducks.configuration.properties;

import com.seiama.javaducks.util.maven.MavenHashType;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.maven")
@NullMarked
public record MavenConfiguration(
  Repositories repositories,
  @DefaultValue({
    "SHA256",
    "SHA1",
    "MD5"
  })
  List<MavenHashType> hashes
) {
  @NullMarked
  public record Repositories(
    Map<String, Group> groups,
    Map<String, Proxied> proxied
  ) {
    public Collection<? extends Repository> all() {
      return this.proxied.values();
    }

    public @Nullable Repository get(final String id) {
      final Group group = this.groups.get(id);
      if (group != null) {
        return group;
      }
      final Proxied proxied = this.proxied.get(id);
      if (proxied != null) {
        return proxied;
      }
      return null;
    }

    @NullMarked
    public sealed interface Repository permits Group, Proxied {
    }

    @NullMarked
    public record Group(
      List<String> members
    ) implements Repository {
    }

    @NullMarked
    public record Proxied(
      String url,
      Path cache
    ) implements Repository {
      public URI url(final String url) {
        return URI.create(this.url + url);
      }
    }
  }
}
