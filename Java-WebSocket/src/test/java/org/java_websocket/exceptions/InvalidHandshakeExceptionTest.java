/*
 * Copyright (c) 2010-2020 Nathan Rajlich
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

package org.java_websocket.exceptions;

import static org.junit.Assert.assertEquals;

import org.java_websocket.framing.CloseFrame;
import org.junit.Test;

/**
 * JUnit Test for the InvalidHandshakeException class
 */
public class InvalidHandshakeExceptionTest {

  @Test
  public void testConstructor() {
    InvalidHandshakeException invalidHandshakeException = new InvalidHandshakeException();
    assertEquals("The close code has to be PROTOCOL_ERROR", CloseFrame.PROTOCOL_ERROR,
        invalidHandshakeException.getCloseCode());
    invalidHandshakeException = new InvalidHandshakeException("Message");
    assertEquals("The close code has to be PROTOCOL_ERROR", CloseFrame.PROTOCOL_ERROR,
        invalidHandshakeException.getCloseCode());
    assertEquals("The message has to be the argument", "Message",
        invalidHandshakeException.getMessage());
    Exception e = new Exception();
    invalidHandshakeException = new InvalidHandshakeException("Message", e);
    assertEquals("The close code has to be PROTOCOL_ERROR", CloseFrame.PROTOCOL_ERROR,
        invalidHandshakeException.getCloseCode());
    assertEquals("The message has to be the argument", "Message",
        invalidHandshakeException.getMessage());
    assertEquals("The throwable has to be the argument", e, invalidHandshakeException.getCause());
    invalidHandshakeException = new InvalidHandshakeException(e);
    assertEquals("The close code has to be PROTOCOL_ERROR", CloseFrame.PROTOCOL_ERROR,
        invalidHandshakeException.getCloseCode());
    assertEquals("The throwable has to be the argument", e, invalidHandshakeException.getCause());

  }

  @Test
  public void testExtends() {
    InvalidHandshakeException invalidHandshakeException = new InvalidHandshakeException();
    assertEquals("InvalidHandshakeException must extend InvalidDataException", true,
        invalidHandshakeException instanceof InvalidDataException);
  }
}
