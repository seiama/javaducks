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

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpPathResolverTest {
  @Test
  void test() {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/javadoc/io.netty/netty-buffer/4.1.106.Final/io/netty/buffer/AbstractByteBufAllocator.html");
    request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/javadoc/{groupId}/{artifactId}/{version}/**");
    assertEquals("io/netty/buffer/AbstractByteBufAllocator.html", HttpPathResolver.resolvePathBasedOnPattern(request));
  }
}
