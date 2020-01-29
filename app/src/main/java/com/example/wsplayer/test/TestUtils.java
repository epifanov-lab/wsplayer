package com.example.wsplayer.test;

import android.content.Context;
import android.net.Uri;
import android.view.TextureView;

import com.example.wsplayer.player.BinarySocketDataSource;
import com.example.wsplayer.utils.schedulers.Schedulers;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpUtilsInternal;
import okhttp3.logging.HttpLoggingInterceptor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;


/**
 * @author Konstantin Epifanov
 * @since 29.01.2020
 */
public class TestUtils {

  public static final String CONFIG_PROD = "{\n" +
    "  \"api\": {\n" +
    "    \"connect\": \"https://api.webka.com\",\n" +
    "     \"apipath\": \"/api/v1/\"\n" +
    "  },\n" +
    "  \"ws_router\": {\n" +
    "    \"connect\": \"https://webka.com/\",\n" +
    "    \"path\": \"/wss\"\n" +
    "  },\n" +
    "  \"storage\": {\n" +
    "    \"domain\": \"https://storage.webka.com\"\n" +
    "  },\n" +
    "  \"www\": {\n" +
    "    \"domain\": \"https://webka.com/\"\n" +
    "  }\n" +
    "}\n";

  public static final String CONFIG_INT = "{\n" +
    "  \"api\": {\n" +
    "    \"connect\": \"https://api.int.rtt.space\",\n" +
    "    \"apipath\": \"/api/v1/\"\n" +
    "  },\n" +
    "  \"ws_router\": {\n" +
    "    \"connect\": \"https://int.rtt.space/\",\n" +
    "    \"path\": \"/wss\"\n" +
    "  },\n" +
    "  \"storage\": {\n" +
    "    \"domain\": \"https://storage.rtt.space\"\n" +
    "  },\n" +
    "  \"www\": {\n" +
    "    \"domain\": \"https://int.rtt.space/\",\n" +
    "    \"stun\": [      \"stun.l.google.com:19302\"\n" +
    "    ],\n" +
    "    \"turn\": [      \"udp://rtt:rttpass@95.211.25.98:3478\",      \"tcp://rtt:rttpass@95.211.25.98:3478\"\n" +
    "    ]\n" +
    "  }\n" +
    "}";

  public static Call.Factory http(Context context, HttpLoggingInterceptor.Logger logger) {
    final long timeouts = 2500L;

    final ExecutorService executor = (ExecutorService) Schedulers.IO_EXECUTOR;
    final Dispatcher dispatcher = new Dispatcher(executor);
    dispatcher.setMaxRequests(64);
    dispatcher.setMaxRequestsPerHost(32);

    OkHttpClient.Builder client = new OkHttpClient.Builder()
      .retryOnConnectionFailure(true)
      .followRedirects(false)
      .followSslRedirects(false)
      //.callTimeout(timeouts, MILLISECONDS)
      .connectTimeout(timeouts, MILLISECONDS)
      .readTimeout(timeouts, MILLISECONDS)
      .writeTimeout(timeouts, MILLISECONDS)
      .cache(new Cache(context.getCacheDir(), 10 * 1024 * 1024))
      .dispatcher(dispatcher)
      .connectionPool(new ConnectionPool(8, 10000L, MILLISECONDS))
      .addInterceptor(new HttpLoggingInterceptor(logger).setLevel(HttpLoggingInterceptor.Level.BASIC));

    Predicate<HttpUrl> predicate = url -> (url.toString().contains("get_stream") || url.toString().contains("nanocosmos"));

    return OkHttpUtilsInternal.factory(client, predicate);
  }

  public static Player testRunHlsPlayer(Context context, TextureView texture, String url) {

    String user = Util.getUserAgent(context, "Webka");
    DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory(user);
    HlsMediaSource source = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(url));

    SimpleExoPlayer player = new SimpleExoPlayer.Builder(context).build();
    player.setVideoTextureView(texture);
    player.prepare(source);
    player.setPlayWhenReady(true);

    return player;
  }

  public static Player testRunWsPlayer(Context context, Call.Factory http, HttpLoggingInterceptor.Logger logger,
                                       TextureView texture, Translation translation) {

    DataSource.Factory dsFactory = () -> new BinarySocketDataSource(http, logger, translation.stramWsToken, translation.streamMediaId);
    ProgressiveMediaSource.Factory pmsFactory = new ProgressiveMediaSource.Factory(dsFactory);
    ProgressiveMediaSource source = pmsFactory.createMediaSource(Uri.parse(translation.streamWsAddr));

    SimpleExoPlayer player = new SimpleExoPlayer.Builder(context).build();
    player.setVideoTextureView(texture);
    player.prepare(source);
    player.setPlayWhenReady(true);

    return player;
  }
}
