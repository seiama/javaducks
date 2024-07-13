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
package com.seiama.javaducks.controller;

import com.seiama.javaducks.service.JavadocService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class JavadocControllerTest {

  @Autowired
  private JavadocService javadocService;

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    this.javadocService.refreshAll();
  }

  @Test
  void testRedirectToPathWithTrailingSlash() throws Exception {
    this.mockMvc.perform(get("/paper/1.12"))
      .andExpect(status().isFound())
      .andExpect(redirectedUrl("/paper/1.12/"));
  }

  @Test
  void testSnapshot() throws Exception {
    this.mockMvc.perform(get("/paper/1.12/"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.TEXT_HTML))
      .andExpect(content().string(containsString("Paper-API 1.12.2-R0.1-SNAPSHOT API")));
  }

  @Test
  void testRelease() throws Exception {
    this.mockMvc.perform(get("/paperlib/1.0.8/"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.TEXT_HTML))
      .andExpect(content().string(containsString("paperlib 1.0.8 API")));
  }

  @Test
  void testRedirect() throws Exception {
    this.mockMvc.perform(get("/paper/1.20/"))
      .andExpect(status().isFound())
      .andExpect(redirectedUrl("/paper/1.20.6/"));
  }

  @Test
  void testVersionAfterRedirect() throws Exception {
    this.mockMvc.perform(get("/paper/1.20.4/"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.TEXT_HTML))
      .andExpect(content().string(containsString("paper-api 1.20.4-R0.1-SNAPSHOT API")));
  }

  @Test
  void testCssFile() throws Exception {
    this.mockMvc.perform(get("/paper/1.12/stylesheet.css"))
      .andExpect(status().isOk())
      .andExpect(content().contentType("text/css"))
      .andExpect(content().string(containsString("Javadoc style sheet")));
  }

  @Test
  void testJsFile() throws Exception {
    this.mockMvc.perform(get("/paper/1.12/script.js"))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/javascript"))
      .andExpect(content().string(containsString("Please contact Oracle")));
  }

  @Test
  void testDuckInjection() throws Exception {
    this.mockMvc.perform(get("/paper/1.12/"))
      .andExpect(status().isOk())
      // test the water of the duck exists
      .andExpect(content().string(containsString("<!--   ~'`~'`~'`~'`~           |__/                                                  -->")));
  }

  @Test
  void testOutdatedBannerInjection() throws Exception {
    this.mockMvc.perform(get("/paper/1.12/overview-summary.html"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("location.href.replace('paper/1.12/', 'paper/1.21/')")));
  }

  @Test
  void testOutdatedBannerInjectionLatest() throws Exception {
    this.mockMvc.perform(get("/paper/1.20.4/overview-summary.html"))
      .andExpect(status().isOk())
      .andExpect(content().string(not(containsString("outdated-banner"))));
  }

  @Test
  void testFaviconInjection() throws Exception {
    this.mockMvc.perform(get("/paper/1.12/"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("<link rel=\"icon\" href=\"/paper/favicon.ico\" />")));
  }

  @Test
  void testFavicon() throws Exception {
    // "mock" favicon
    final Path favicon = this.javadocService.faviconFor("paper");
    Files.createDirectories(favicon.getParent());
    Files.write(favicon, "duck".getBytes());

    this.mockMvc.perform(get("/paper/favicon.ico"))
      .andExpect(status().isOk())
      .andExpect(content().contentType("image/x-icon"));
  }

  @Test
  void testMissingFavicon() throws Exception {
    this.mockMvc.perform(get("/paperlib/favicon.ico"))
      .andExpect(status().isNotFound());
  }
}
