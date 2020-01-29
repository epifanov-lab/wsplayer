/*
 * BinarySocketDataSource.java
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

import android.net.Uri;

import com.example.wsplayer.utils.Json;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import org.json.JSONObject;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.logging.HttpLoggingInterceptor;


/**
 * Binary Socket Data Source.
 *
 * @author Gleb Nikitenko
 * @since 13.12.19
 **/
public final class BinarySocketDataSource extends BaseDataSource {

  /** OkHttp Client. */
  private final Call.Factory mSocketFactory;

  /** WS Logger. */
  private final HttpLoggingInterceptor.Logger mLogger;

  /** Binary Socket Instance. */
  private BinaryWebSocket mSocket = null;

  /** Current Uri. */
  private Uri mUri = null;

  /** Opened State. */
  private boolean mOpened = false;

  /** Remaining bytes. */
  private int mRemaining = 0;

  /** Current frame. */
  private byte[] mFrame = null;

  private final String mToken;
  private final long mMediaId;

  /**
   * Constructs a new {@link BinarySocketDataSource}
   *
   * @param factory binary socket factory
   * @param logger http logger
   */
  public BinarySocketDataSource(@NonNull Call.Factory factory,
                                @NonNull HttpLoggingInterceptor.Logger logger,
                                @Nullable String token, long mediaId) {
    super(true);
    mSocketFactory = factory;
    mLogger = logger;
    mToken = token;
    mMediaId = mediaId;
  }

  /** {@inheritDoc} */
  @Override
  public final long open(@NonNull DataSpec dataSpec) throws IOException {
    final String url = (mUri = dataSpec.uri).toString();
    transferInitializing(dataSpec);
    mSocket = BinaryWebSocket.create(mSocketFactory, url, mLogger);
    mOpened = true;
    try {
      return C.LENGTH_UNSET;
    } finally {
      if (mToken != null && mMediaId != 0) sendToken();
      transferStarted(dataSpec);
    }
  }

  private void sendToken() {
    try {
      JSONObject json = Json.newJson(body -> body
        .put("token", mToken)
        .put("mediaId", mMediaId));
      mSocket.write(json.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final int read(@NonNull byte[] buffer, int offset, int length) throws IOException {
    System.out.println("BinarySocketDataSource.read: buffer = [" + buffer + "], offset = [" + offset + "], length = [" + length + "]");
    if (length == 0) return 0;
    // We've read all of the data from the current packet. Get another.
    if (mRemaining == 0) {
      mRemaining = (mFrame = mSocket.read()).length;
      if (mFrame == BinaryWebSocket.END_OF_STREAM) return C.RESULT_END_OF_INPUT;
      bytesTransferred(mRemaining);
    }
    final int frameOffset = mFrame.length - mRemaining;
    final int result = Math.min(mRemaining, length);
    System.arraycopy(mFrame, frameOffset, buffer, offset, result);
    mRemaining -= result;
    return result;
  }

  /** {@inheritDoc} */
  @Override
  public final void close() throws IOException {
    System.out.println("BinarySocketDataSource.close");
    mFrame = null;
    mRemaining = 0;
    mUri = null;
    if (mSocket != null) {
      mSocket.close();
      mSocket = null;
    }
    if (!mOpened) return;
    mOpened = false;
    transferEnded();
  }

  /** {@inheritDoc} */
  @Nullable @Override
  public final Uri getUri() {
    return mUri;
  }

}
