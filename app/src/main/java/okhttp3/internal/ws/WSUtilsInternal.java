/*
 * WSUtilsInternal.java
 * internals
 *
 * Copyright (C) 2019, Realtime Technologies Ltd. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the
 * property of Realtime Technologies Limited and its SUPPLIERS, if any.
 *
 * The intellectual and technical concepts contained herein are
 * proprietary to Realtime Technologies Limited and its suppliers and
 * may be covered by Russian Federation and Foreign Patents, patents
 * in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Realtime Technologies Limited.
 */

package okhttp3.internal.ws;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.OkioUtilsInternal;
import okio.Sink;

import static okhttp3.internal.ws.WebSocketProtocol.OPCODE_BINARY;
import static okhttp3.internal.ws.WebSocketProtocol.OPCODE_CONTROL_CLOSE;
import static okhttp3.internal.ws.WebSocketProtocol.OPCODE_CONTROL_PING;
import static okhttp3.internal.ws.WebSocketProtocol.OPCODE_CONTROL_PONG;
import static okhttp3.internal.ws.WebSocketProtocol.OPCODE_TEXT;
import static okio.ByteString.encodeUtf8;


/**
 * @author Nikitenko Gleb
 * @since 1.0, 19/08/2019
 */
@SuppressWarnings("ALL")
public final class WSUtilsInternal {

  /**
   * @param client  is a client
   * @param sink    buffered sink
   * @param random  random generator
   *
   * @return web socket writer
   */
  public static Function<Frame, IORunnable> writer
  (boolean client, BufferedSink sink, Random random) {
    final WebSocketWriter writer = new WebSocketWriter(client, sink, random);
    return new Function<Frame, IORunnable>() {
      @Override
      public IORunnable apply(Frame frame) {
        System.out.println("WSUtilsInternal.writer.apply: frame = [" + frame + "]");
        return new IORunnable() {
          @Override
          public void run() throws IOException {frame.send(writer);}
        };
      }
    };
  }

  /**
   * @param client    is a client
   * @param callback  messages callback
   * @param source    buffered source
   *
   * @return next puller
   */
  public static IORunnable reader
  (boolean client, BufferedSource source, Consumer<Frame> callback) {
    final WebSocketReader reader = new WebSocketReader(client, source,
        new WebSocketReader.FrameCallback() {
          @Override
          public void onReadMessage(String value) {
            System.out.println("WSUtilsInternal.onReadMessage: value = [" + value + "]");
            callback.accept(new Text(value));
          }

          @Override
          public void onReadMessage(ByteString value) {
            System.out.println("WSUtilsInternal.onReadMessage: value = [" + value + "]");
            callback.accept(new Binary(OkioUtilsInternal.bytes(value)));
          }

          @Override
          public void onReadClose(int code, String value) {
            System.out.println("WSUtilsInternal.onReadClose: code = [" + code + "], value = [" + value + "]");
            callback.accept(new Close(code, value));
          }

          @Override
          public void onReadPing(ByteString value) {
            System.out.println("WSUtilsInternal.onReadPing: value = [" + value + "]");
            callback.accept(new Ping(value));
          }

          @Override
          public void onReadPong(ByteString value) {
            System.out.println("WSUtilsInternal.onReadPong: value = [" + value + "]");
            callback.accept(new Pong(value));
          }

        });
    return () -> {reader.processNextFrame();};
  }

  /** Websocket Base Frame. */
  public static abstract class Frame {

    /** Op code. */
    final int op;

    /**
     * Constructs a new {@link Frame}.
     *
     * @param op op code
     */
    private Frame(int op) {this.op = op;}

    /**
     * @param writer web-socket writer
     *
     * @throws IOException write failed
     */
    abstract void send(WebSocketWriter writer) throws IOException;

  }

  /** Message frame. */
  private static abstract class Message extends Frame {

    /**
     * Constructs a new {@link Message}.
     *
     * @param op op code
     */
    private Message(int op) {super(op);}

    /** {@inheritDoc} */
    @Override
    final void send(WebSocketWriter writer) throws IOException {
      final Buffer buffer = buffer();
      final long size = buffer.size();
      try (final Sink sink = writer.newMessageSink(op, size))
      {sink.write(buffer, size);}
    }

    /** @return prepare for send buffer */
    abstract Buffer buffer();
  }

  /** Text Message Frame. */
  public static final class Text extends Message {

    /** String content. */
    public final String content;

    /**
     * Constructs a new {@link Text}.
     *
     * @param value text value.
     */
    public Text(String value) {
      super(OPCODE_TEXT);
      content = value;
    }

    /** {@inheritDoc} */
    @Override
    final Buffer buffer()
    {return new Buffer().writeUtf8(content);}

    /** {@inheritDoc} */
    @Override
    public final String toString()
    {return /*"txt :" +*/ content;}
  }

  /** Binary Message Frame. */
  public static final class Binary extends Message {

    /** Bytes content. */
    public final byte[] content;

    /**
     * Constructs a new {@link Binary}.
     *
     * @param value bytes value
     */
    public Binary(byte[] value) {
      super(OPCODE_BINARY);
      content = value;
    }

    /** {@inheritDoc} */
    @Override
    final Buffer buffer()
    {return new Buffer().write(content);}

    /** {@inheritDoc} */
    @Override
    public final String toString()
    {return "bin :" + content.length; }
  }


  /** Ping Pong Frame */
  private static abstract class PingPong extends Frame {

    /** PingPong content. */
    public final ByteString content;

    /**
     * Constructs a new {@link Frame}.
     *
     * @param op op code
     * @param content content
     */
    private PingPong(int op, ByteString content)
    {super(op); this.content = content;}
  }

  /** Ping Frame Message. */
  public static final class Ping extends PingPong {

    /**
     * Constructs a new {@link Ping}.
     *
     * @param content content
     */
    public Ping(ByteString content) {super(OPCODE_CONTROL_PING, content);}

    /** {@inheritDoc} */
    @Override
    final void send(WebSocketWriter writer) throws IOException
    {writer.writePing(content);}

    /** {@inheritDoc} */
    @Override
    public final String toString()
    {return "ping:" + content;}

  }

  /** Pong Frame Message. */
  public static final class Pong extends PingPong {

    /**
     * Constructs a new {@link Pong}.
     *
     * @param content content
     */
    public Pong(ByteString content) {super(OPCODE_CONTROL_PONG, content);}

    /** {@inheritDoc} */
    @Override
    final void send(WebSocketWriter writer) throws IOException
    {writer.writePong(content);}

    /** {@inheritDoc} */
    @Override
    public final String toString()
    {return "pong:" + content;}
  }

  /** Close frame */
  public static final class Close extends Frame {

    /** Close code. */
    public final int code;

    /** Close reason. */
    public final String reason;

    /**
     * Constructs a new {@link Frame}.
     *
     * @param code close code
     * @param reason reason
     */
    public Close(int code, String reason) {
      super(OPCODE_CONTROL_CLOSE);
      this.code = code; this.reason = reason;
    }

    /** {@inheritDoc} */
    @Override
    final void send(WebSocketWriter writer) throws IOException
    {writer.writeClose(code, reason != null ? encodeUtf8(reason) : null);}

    /** {@inheritDoc} */
    @Override
    public final String toString()
    {return "close:" + code + (reason != null && !reason.isEmpty() ? (" " + reason) : "");}

  }

  /** Input/Output Operation. */
  @FunctionalInterface
  public interface IORunnable {

    /** @throws IOException io failure */
    void run() throws IOException;

    /** @return callable equivalient */
    default Callable<Void> callable()
    {return () -> {try {return null;} finally {run();}};}
  }
}
