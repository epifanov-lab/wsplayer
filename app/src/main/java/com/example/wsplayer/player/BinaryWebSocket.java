/*
 * BinaryWebSocket.java
 * webka
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

package com.example.wsplayer.player;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.util.Random;
import java.util.function.Function;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Internal;
import okhttp3.internal.connection.Exchange;
import okhttp3.internal.ws.RealWebSocket.Streams;
import okhttp3.internal.ws.WSUtilsInternal.Binary;
import okhttp3.internal.ws.WSUtilsInternal.Close;
import okhttp3.internal.ws.WSUtilsInternal.Frame;
import okhttp3.internal.ws.WSUtilsInternal.IORunnable;
import okhttp3.internal.ws.WSUtilsInternal.Text;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.ByteString;

import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static okhttp3.internal.ws.WSUtilsInternal.reader;
import static okhttp3.internal.ws.WSUtilsInternal.writer;
import static okhttp3.internal.ws.WebSocketProtocol.acceptHeader;


/**
 * @author Gleb Nikitenko
 * @since 13.12.19
 **/
interface BinaryWebSocket extends Closeable {

  /** System constants. */
  byte[]
    END_OF_STREAM = {},
    UNKNOWN_FRAME = {};

  /**
   * @return new incoming frame
   *
   * @throws IOException read exception
   */
  @NonNull byte[] read() throws IOException;

  /**
   * @param value write value
   *
   * @throws IOException read exception
   */
  void write(@NonNull String value) throws IOException;

  @NonNull
  static BinaryWebSocket create(@NonNull Call.Factory client, @NonNull String url,
                                @NonNull HttpLoggingInterceptor.Logger logger) throws IOException {
    final Frame[] frames = new Frame[1]; final Request request = request(url);
    final Call call = client.newCall(request); try (final Response response = call.execute()) {
      checkUpgrade(response.headers(), requireNonNull(request.tag(String.class)));
      final Streams streams = streams(exchange(response), logger);
      final Random random = requireNonNull(request.tag(Random.class));
      final Function<Frame, IORunnable> writer = writer(streams.client, streams.sink, random);
      final IORunnable reader = reader(streams.client, streams.source, v -> frames[0] = v);
      return new BinaryWebSocket() {
        @Override
        public final void write(@NonNull String value) throws IOException {
          System.out.println("BinaryWebSocket.write: value = [" + value + "]");
          final Frame f = new Text(value);
          writer.apply(f).run();
          log(logger, true, f);
        }

        @NonNull
        @Override
        public final byte[] read() throws IOException {
          System.out.println("BinaryWebSocket.read");
          reader.run();
          final Frame frame = frames[0];
          frames[0] = null;
          log(logger, false, frame);
          if (frame instanceof Binary) return ((Binary) frame).content;
          else if (frame instanceof Close) return END_OF_STREAM;
          else return UNKNOWN_FRAME;
        }

        @Override
        public final void close() throws IOException {
          System.out.println("BinaryWebSocket.close");
          try {
            final Frame f = new Close(1000, null);
            writer.apply(f).run();
            log(logger, true, f);
          } catch (IOException ignored) {} finally {
            try {streams.close();} finally {frames[0] = null;}
          }
        }
      };
    }
  }

  /**
   * @param url connect url
   *
   * @return http request
   */
  @NonNull static Request request(@NonNull String url) {
    final Random random = new Random();
    final String key = key(random);
    return
      new Request.Builder().url(url)
        .header("Upgrade", "websocket")
        .header("Connection", "Upgrade")
        .header("Sec-WebSocket-Key", key)
        .header("Sec-WebSocket-Version", "13")
        .header("Sec-WebSocket-Extensions", "permessage-deflate; client_max_window_bits")
        .header("Accept-Encoding", "gzip, deflate, br")
        .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
        .tag(String.class, acceptHeader(key))
        .tag(Random.class, random)
        .build();
  }

  /**
   * @param headers response headers
   * @param key     accept secure key
   * @throws ProtocolException something wrong
   */
  static void checkUpgrade(@NonNull Headers headers, @NonNull String key) throws ProtocolException {
    final String
      conFmt = "Expected 'Connection' header value 'Upgrade' but was '%s'",
      upgFmt = "Expected 'Upgrade' header value 'websocket' but was '%s'",
      accFmt = "Expected 'Sec-WebSocket-Accept' header value '%s' but was '%s'";

    final String exception; String header;
    if (!"Upgrade".equalsIgnoreCase(header = headers.get("Connection")))
      exception = String.format(conFmt, header);
    else if (!"websocket".equalsIgnoreCase(header = headers.get("Upgrade")))
      exception = String.format(upgFmt, header);
    else if (!key.equals(header = headers.get("Sec-WebSocket-Accept")))
      exception = String.format(accFmt, header, key);
    else exception = null;
    if (exception != null) throw new ProtocolException(exception);
  }


  /**
   * @param random random generator
   * @return ws signature key
   */
  static String key(Random random) {
    final byte[] nonce = new byte[16];
    random.nextBytes(nonce);
    return ByteString.of(nonce).base64();
  }

  /**
   * @param response socket response
   * @return response-based exchange
   *
   * @throws ProtocolException something wrong
   */
  @NonNull static Exchange exchange(@NonNull Response response) throws ProtocolException {
    final Exchange result = Internal.instance.exchange(response);
    if (result == null) throw new ProtocolException("Web Socket exchange missing");
    return result;
  }

  /**
   * @param exchange socket exchange
   * @param logger socket logger
   * @return websocket streams
   *
   * @throws SocketException open exception
   */
  @NonNull static Streams streams(@NonNull Exchange exchange,
                                  @Nullable HttpLoggingInterceptor.Logger logger)
    throws SocketException {
    final Streams streams = exchange.newWebSocketStreams();
    return new Streams(streams.client, streams.source, streams.sink) {
      {log(logger, true);}
      @Override
      public final void close() throws IOException
      {streams.close(); log(logger, false);}
    };
  }

  /**
   * @param logger http logger interceptor
   * @param opened open/close
   */
  static void log(@Nullable HttpLoggingInterceptor.Logger logger, boolean opened) {
    if (logger == null) return;
    final StringBuilder result = new StringBuilder(currentThread().getName());
    result
      .append(" | ").append("===\t")
      .append("WEBSOCKET ")
      .append(opened ? "OPENED" : "CLOSED");
    logger.log(result.toString()); result.setLength(0);
  }

  /**
   * @param logger websocket logger
   * @param send  transport direction
   * @param frame transported frame
   */
  static void log(@NonNull HttpLoggingInterceptor.Logger logger, boolean send, @NonNull Frame frame) {
    final StringBuilder result = new StringBuilder(currentThread().getName());
    result.append(" | ").append(send ? "-->" : "<--").append('\t').append(frame);
    logger.log(result.toString()); result.setLength(0);
  }
}
