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
package com.seiama.javaducks.util.http;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;

@NullMarked
public final class MediaTypes {
  public static final String APPLICATION_JAVA_ARCHIVE_VALUE = "application/java-archive";
  public static final MediaType APPLICATION_JAVA_ARCHIVE = MediaType.parseMediaType(APPLICATION_JAVA_ARCHIVE_VALUE);

  public static final String APPLICATION_JAVASCRIPT_VALUE = "application/javascript";
  public static final MediaType APPLICATION_JAVASCRIPT = MediaType.parseMediaType(APPLICATION_JAVASCRIPT_VALUE);

  public static final String APPLICATION_ZIP_VALUE = "application/zip";
  public static final MediaType APPLICATION_ZIP = MediaType.parseMediaType(APPLICATION_ZIP_VALUE);

  public static final String TEXT_CSS_VALUE = "text/css";
  public static final MediaType TEXT_CSS = MediaType.parseMediaType(TEXT_CSS_VALUE);

  private MediaTypes() {
  }

  public static @Nullable MediaType fromFileName(final String name) {
    final int index = name.lastIndexOf('.');
    if (index != -1) {
      return fromFileExtension(name.substring(index + 1));
    }
    return null;
  }

  public static @Nullable MediaType fromFileExtension(final String extension) {
    return switch (extension) {
      case "css" -> TEXT_CSS;
      case "java" -> APPLICATION_JAVA_ARCHIVE;
      case "js" -> APPLICATION_JAVASCRIPT;
      case "zip" -> APPLICATION_ZIP;
      default -> null;
    };
  }
}
