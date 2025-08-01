/*
 *  Copyright (c) 2010-2020 Nathan Rajlich
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.java_websocket.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

  private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private final String threadPrefix;
  private final boolean daemon;

  public NamedThreadFactory(String threadPrefix) {
    this.threadPrefix = threadPrefix;
    this.daemon = false;
  }

  public NamedThreadFactory(String threadPrefix, boolean daemon) {
    this.threadPrefix = threadPrefix;
    this.daemon = daemon;
  }

  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = defaultThreadFactory.newThread(runnable);
    thread.setDaemon(daemon);
    thread.setName(threadPrefix + "-" + threadNumber);
    return thread;
  }
}
