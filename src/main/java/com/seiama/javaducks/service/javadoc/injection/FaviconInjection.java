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

import com.seiama.javaducks.service.JavadocService;
import com.seiama.javaducks.service.javadoc.JavadocKey;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class FaviconInjection implements Injection {
  private final JavadocService service;

  @Autowired
  public FaviconInjection(final JavadocService service) {
    this.service = service;
  }

  @Override
  public boolean canInject(final Path file, final JavadocKey key) {
    return file.toString().endsWith(HTML) && this.service.faviconFor(key.project()) != null;
  }

  @Override
  public String inject(final String line, final Path file, final JavadocKey key) {
    if (line.contains("</head>")) {
      return "<link rel=\"icon\" href=\"/%s/favicon.ico\" />\n%s".formatted(key.project(), line);
    }
    return line;
  }
}
