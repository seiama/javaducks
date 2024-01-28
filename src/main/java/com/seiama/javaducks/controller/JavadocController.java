/*
 * This file is part of javaducks, licensed under the MIT License.
 *
 * Copyright (c) 2023 Seiama
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
package com.seiama.javaducks.controller;

import com.seiama.javaducks.service.JavadocService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

@Controller
public class JavadocController {
  // https://regex101.com/r/fyzJ7g/1
  private static final Pattern STATICS_PATTERN = Pattern.compile("^(?!.*search-index).*\\.(js|png|css|html)$");
  private static final CacheControl CACHE_CONTROL = CacheControl.maxAge(Duration.ofMinutes(10));
  private static final CacheControl STATICS_CACHE_CONTROL = CacheControl.maxAge(Duration.ofDays(7));
  private static final ContentDisposition CONTENT_DISPOSITION = ContentDisposition.inline().build();
  private static final Map<String, MediaType> MEDIATYPES = Map.of(
    ".css", MediaType.parseMediaType("text/css"),
    ".js", MediaType.parseMediaType("application/javascript"),
    ".zip", MediaType.parseMediaType("application/zip")
  );
  private final JavadocService service;

  @Autowired
  public JavadocController(final JavadocService service) {
    this.service = service;
  }

  @GetMapping("/{project:[a-z]+}/{version:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}")
  @ResponseBody
  public ResponseEntity<?> redirectToPathWithTrailingSlash(
    final HttpServletRequest request,
    @PathVariable final String project,
    @PathVariable final String version
  ) {
    return status(HttpStatus.FOUND)
      .location(URI.create(request.getRequestURI() + "/"))
      .build();
  }

  @GetMapping("/{project:[a-z]+}/{version:[0-9.]+-?(?:pre|SNAPSHOT)?(?:[0-9.]+)?}/**")
  @ResponseBody
  public ResponseEntity<?> serveJavadocs(
    final HttpServletRequest request,
    @PathVariable final String project,
    @PathVariable final String version
  ) {
    final String root = "/%s/%s".formatted(project, version);
    //noinspection resource - This warning can be ignored, we want to keep this FS open.
    final @Nullable FileSystem fs = this.service.contentsFor(new JavadocService.Key(project, version));
    if (fs != null) {
      String path = ((String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).substring(root.length());
      if (path.equals("/")) {
        path = "index.html";
      }
      final Path file = fs.getPath(path);
      if (Files.isRegularFile(file)) {
        return ok()
          .cacheControl(STATICS_PATTERN.matcher(path).find() ? STATICS_CACHE_CONTROL : CACHE_CONTROL)
          .headers(headers -> {
            headers.setContentDisposition(CONTENT_DISPOSITION);
            headers.set("X-JavaDucks", "Quack");
            final String name = file.getFileName().toString();
            for (final Map.Entry<String, MediaType> entry : MEDIATYPES.entrySet()) {
              if (name.startsWith(entry.getKey())) {
                headers.setContentType(entry.getValue());
                break;
              }
            }
          })
          .body(new FileSystemResource(file));
      }
    }
    return notFound()
      .cacheControl(CacheControl.noCache())
      .build();
  }
}
