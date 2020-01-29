/*
 * Translation.java
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

package com.example.wsplayer.test;

import com.example.wsplayer.utils.Json;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

import reactor.core.publisher.Mono;
import ru.realtimetech.webka.client.Client;


/**
 * Translation structure.
 *
 * @author Gleb Nikitenko
 * @since 10.04.19
 */
@SuppressWarnings({"WeakerAccess", "SpellCheckingInspection"})
public final class Translation {

  /** Translation id. */
  public final String translationId;

  /** Room Id. */
  public final long roomId;

  /** Session id. */
  public final String sessionId;

  /** Chat id. */
  public final int chatId;

  /** Translation name. */
  public final String name;

  /** Translation description. */
  public final String description;

  /** Session name. */
  public final String sessionName;

  /** Session description. */
  public final String sessionDescription;

  /** User Id */
  public final int userId;

  /** User nickname */
  public final String userNickName;

  /** User full name */
  public final String userFullName;

  /** City names */
  public final String cityNamesRu;

  /** Country ISO code */
  public final String countryIsoCode;

  /** User avatar url */
  public final String avatarBigUrl;

  /** Hashtags */
  public final String tagsNamesRu;

  /** View count */
  public final int viewCount;

  /** Stream Width Pixels */
  public final int streamWidth;

  /** Stream Height Pixels */
  public final int streamHeight;

  /** Time stamp. */
  public final String startTime;

  /** Media stream. */
  public final String streamMediaUrl;

  /** Media stream id. */
  public final long streamMediaId;

  public final String stramWsToken;
  public final String streamWsAddr;

  /**
   * Translation Item global position
   * of backend by last response.
   */
  public final int pagingOffset;

  /** Object hash. */
  private final int mHash;

  /**
   * Constructs a new {@link Translation}.
   *
   * @param json json response object
   */
  private Translation(JSONObject json) {
    translationId = Json.optString(json, /*"translationId"*/"sessionId").orElse(null);
    roomId = Json.optLong(json, "roomId").orElse(-1L);
    sessionId = Json.optString(json, "sessionId").orElse(null);
    chatId = Json.optInteger(json, "chatId").orElse(-1);
    name = Json.optString(json, "translationName").orElse("БЕЗЫМЯННАЯ");
    description = Json.optString(json, "translationDescription").orElse("БЕЗЫМЯННАЯ");
    sessionName = Json.optString(json, "sessionName").orElse("БЕЗЫМЯННАЯ");
    sessionDescription = Json.optString(json, "sessionDescription").orElse(null);

    userId = Json.optInteger(json, "userId").orElse(-1);
    userNickName = Json.optString(json, "nickname").orElse("NO_NICKNAME");
    userFullName = Json.optString(json, "fullName").orElse("NO_FULLNAME");

    cityNamesRu = Json.getJsonObject(json, "cityNames")
      .flatMap(object -> Json.getJsonString(object, "ru_RU"))
      .orElse(null);

    countryIsoCode = Json.optString(json, "countryIsoCode").orElse(null);

    pagingOffset = Json.optInteger(json, "pageOffset").orElse(-1) - 1;
    avatarBigUrl = Json.getJsonObject(json, "avatar")
      .flatMap(object -> Json.getJsonObject(object, "big"))
      .flatMap(object -> Json.getJsonString(object, "url"))
      .orElse(null);
    //avatarBigUrl = "https://i.pravatar.cc/54" + (pagingOffset > 0 ? "?img=" + pagingOffset:"");

    tagsNamesRu = Json.getJsonObject(json, "tags")
      .flatMap(object -> Json.getJsonObject(object, "names"))
      .flatMap(object -> Json.getJsonString(object, "ru_RU"))
      .orElse(null);

    viewCount = Json.optInteger(json, "viewCount").orElse(-1);
    startTime = Json.optString(json, "startTime").orElse("NO_TIME");

    //streamMediaUrl = pagingOffset >= HLS.length ? Json.optString(json, "streamMediaUrl").orElse(null) : HLS[pagingOffset];
    //https://rt-uk.secure.footprint.net/1106.m3u8 1600 800 400 64
    //streamMediaUrl = "https://ms-rhls-node-611d9e9dea44.sandbox.rtt.space/hls/live/master/7228914159303005.m3u8";
    //streamMediaUrl = "https://rt-news-gd.secure2.footprint.net/1103_2500Kb.m3u8?" + pagingOffset;
    //streamMediaUrl = "wss://bintu-h5live.nanocosmos.de:443/h5live/stream/stream.mp4?url=rtmp%3A%2F%2Fbintu-play.nanocosmos.de%3A1935%2Fplay&stream=lGMGD-z8Hgb&cid=447875&pid=8884176944";
    streamMediaUrl = Json.optString(json, "streamMediaUrl").orElse(null);

    streamWidth = Json.optInteger(json, "streamWidth").orElse(-1);
    streamHeight = Json.optInteger(json, "streamHeight").orElse(-1);
    streamMediaId = Json.optLong(json, "streamMediaId").orElse(-1);

    stramWsToken = Json.getJsonObject(json, "streamWs")
                       .flatMap(object -> Json.optString(object, "token"))
                       .orElse(null);

    streamWsAddr = Json.getJsonObject(json, "streamWs")
                       .flatMap(object -> Json.optString(object, "addr"))
                       .orElse(null);

    mHash = translationId.hashCode();
  }

  /**
   * @param json   json array
   *
   * @return set of translations
   */
  static Translation[] just(JSONArray json) {
    return Json.objects(json)
      .map(Translation::new)
      .toArray(Translation[]::new);
  }

  public static Mono<Void> size(Client client, int width, int height) {
    return Mono.from(client.put("translation/size", "width", width, "height", height)).then();
  }

  public static Mono<Translation> start(Client client, int systemTagId, String name) {
    return Mono.from(client.post("translation", "status", "ONLINE", "type", "PUBLIC","systemTagId", systemTagId, "name", name))
               .map(Json::object).map(Translation::new);
  }

  public static Mono<String> stop(Client client, long roomId, String translationId){
    return Mono.from(client.put("translation/stop", "roomId", roomId, "translationId", translationId));
  }


  public static Mono<String> delete(Client client, String translationId){
    return Mono.from(client.delete("translation","translationId", translationId));
  }

  public static Mono<Translation> bySessionId(Client client, String sessionId) {
    return Mono.from(client.get("translation/session", "sessionId", sessionId))
               .map(Json::object).map(Translation::new);
  }

  public static Mono<Translation[]> videoMain(Client client, int offset, int limit) {
    final String cover = client.storage("1");
    return Mono.from(client.get("video/main", "offset", offset, "limit", limit))
      //Mono.just(STUB)
      .map(Json::array).map(Translation::just)
      //.doOnNext(Collections::reverse)
      ;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final Translation translation = (Translation) obj;
    return mHash == translation.mHash &&
      Objects.equals(sessionId, translation.sessionId) &&
      Objects.equals(roomId, translation.roomId) &&
      Objects.equals(chatId, translation.chatId) &&
      Objects.equals(name, translation.name) &&
      Objects.equals(description, translation.description) &&
      Objects.equals(userNickName, translation.userNickName) &&
      Objects.equals(userFullName, translation.userFullName) &&
      Objects.equals(viewCount, translation.viewCount) &&
      Objects.equals(streamWidth, translation.streamWidth) &&
      Objects.equals(streamHeight, translation.streamHeight) &&
      Objects.equals(streamMediaId, translation.streamMediaId) &&
      Objects.equals(startTime, translation.startTime) &&
      Objects.equals(streamMediaUrl, translation.streamMediaUrl)
      ;
  }

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    return mHash;
  }

  @Override
  public String toString() {
    return "Translation{" + "translationId='" + translationId + '\'' + ", roomId=" + roomId
      + ", sessionId='" + sessionId + '\'' + ", chatId=" + chatId + ", name='" + name + '\''
      + ", description='" + description + '\'' + ", sessionName='" + sessionName + '\''
      + ", sessionDescription='" + sessionDescription + '\'' + ", userId=" + userId
      + ", userNickName='" + userNickName + '\'' + ", userFullName='" + userFullName + '\''
      + ", cityNamesRu='" + cityNamesRu + '\'' + ", countryIsoCode='" + countryIsoCode + '\''
      + ", avatarBigUrl='" + avatarBigUrl + '\'' + ", tagsNamesRu='" + tagsNamesRu + '\''
      + ", viewCount=" + viewCount + ", streamWidth=" + streamWidth + ", streamHeight="
      + streamHeight + ", startTime='" + startTime + '\'' + ", streamMediaUrl='" + streamMediaUrl
      + '\'' + ", streamMediaId=" + streamMediaId + ", stramWsToken='" + stramWsToken + '\''
      + ", streamWsAddr='" + streamWsAddr + '\'' + ", pagingOffset=" + pagingOffset + ", mHash="
      + mHash + '}';
  }

}
