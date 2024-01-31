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
package com.seiama.javaducks.util.crypto;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record HashAlgorithm(
  String name,
  HashFunction function
) {
  @SuppressWarnings("deprecation")
  public static final HashAlgorithm MD5 = new HashAlgorithm("md5", Hashing.md5());
  @SuppressWarnings("deprecation")
  public static final HashAlgorithm SHA1 = new HashAlgorithm("sha1", Hashing.sha1());
  public static final HashAlgorithm SHA256 = new HashAlgorithm("sha256", Hashing.sha256());
  public static final HashAlgorithm SHA512 = new HashAlgorithm("sha512", Hashing.sha512());
}
