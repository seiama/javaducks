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

import com.seiama.javaducks.injection.Injection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InjectionService {

  private static final Logger LOG = LoggerFactory.getLogger(InjectionService.class);

  private final List<Injection> injections;

  public InjectionService(final List<Injection> injections) {
    this.injections = injections;
  }

  public Object runInjections(final Path file, final String project, final String version) {
    final List<Injection> applicableInjections = this.injections.stream().filter(injection -> injection.canInject(file)).toList();
    if (applicableInjections.isEmpty()) {
      return new FileSystemResource(file);
    }

    try (final Stream<String> lines = Files.lines(file)) {
      return lines.map(l -> {
        String line = l;
        for (final Injection injection : applicableInjections) {
          line = injection.inject(line, file, project, version);
        }
        return line;
      }).collect(Collectors.joining("\n"));
    } catch (final IOException e) {
      LOG.error("Could not read file", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file");
    }
  }
}
