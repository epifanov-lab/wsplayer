/*
 * Json.java
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

package com.example.wsplayer.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.stream.IntStream.range;
import static org.json.JSONObject.NULL;


/**
 * @author Gleb Nikitenko
 * @since 25.03.19
 */
@SuppressWarnings("WeakerAccess")
public final class Json {

  /** Empty object */
  public static final JSONObject OBJECT_EMPTY = object("{}");

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private Json() {
    throw new AssertionError();
  }

  /**
   * @param json json builder
   *
   * @return json object
   */
  public static JSONObject newJson(JsonBuilder json) {
    return new JSONObject() {{
      try {json.construct(this);}
      catch (JSONException exception)
      {throw new IllegalArgumentException(exception);}
    }};
  }

  /**
   * @param value string representation
   *
   * @return json array structure
   */
  public static JSONArray array(String value) {
    try {return new JSONArray(value);}
    catch (JSONException exception)
    {throw new IllegalArgumentException(exception);}
  }

  /**
   * @param value string representation
   *
   * @return json object structure
   */
  public static JSONObject object(String value) {
    try {return new JSONObject(value);}
    catch (JSONException exception)
    {throw new IllegalArgumentException(exception);}
  }

  /**
   * @param value json array
   *
   * @return object stream
   */
  public static Stream<JSONObject> objects(JSONArray value)
  {return stream(value, value::optJSONObject);}

  /**
   * @param value json array
   *
   * @return arrays stream
   */
  public static Stream<JSONArray> arrays(JSONArray value)
  {return stream(value, value::optJSONArray);}

  /**
   * @param value  json array
   * @param mapper item mapper
   *
   * @param <T> item type
   *
   * @return stream of items
   */
  private static <T> Stream<T> stream(JSONArray value, IntFunction<T> mapper)
  {return range(0, value.length()).mapToObj(mapper);}

  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  public static Optional<JSONArray> getArray(JSONObject object, String key)
  {return get(object, key).map(v -> (JSONArray) v);}

  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  public static Optional<JSONObject> getObject(JSONObject object, String key)
  {return get(object, key).map(v -> (JSONObject) v);}

  /**
   * @param array json array
   * @param index array index
   *
   * @return value
   */
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public static JSONObject getObject(JSONArray array, int index) {
    try {return optObject(array, index).get();}
    catch (NoSuchElementException exception)
    {return throwKeyNotFound(array, index);}
  }

  /**
   * @param array json array
   * @param index array index
   *
   * @return optional value
   */
  public static Optional<JSONObject> optObject(JSONArray array, int index)
  {return get(array, index).map(v -> (JSONObject) v);}

  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public static boolean getBoolean(JSONObject object, String key) {
    try {return optBoolean(object, key).get();}
    catch (NoSuchElementException exception)
    {return throwKeyNotFound(object, key);}
  }


  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  public static Optional<Boolean> optBoolean(JSONObject object, String key) {
    return get(object, key).flatMap(value -> {
      if (value instanceof Boolean)
        return Optional.of((Boolean) value);
      else if (value instanceof String) {
        final String string = (String) value;
        if ("true".equalsIgnoreCase(string)) return Optional.of(true);
        else if ("false".equalsIgnoreCase(string)) return Optional.of(false);
        else return Optional.empty();
      } else return Optional.empty();
    });
  }

  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public static String getString(JSONObject object, String key) {
    try {return optString(object, key).get();}
    catch (NoSuchElementException exception)
    {return throwKeyNotFound(object, key);}
  }

  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  public static Optional<String> optString(JSONObject object, String key) {
    return get(object, key).map(value -> {
      if (value instanceof String)
        return (String) value;
      else return String.valueOf(value);
    });
  }

  /**
   * @param json json object
   * @param key    json key
   * @param fallback fallback
   *
   * @return optional value
   */
  public static String optString(JSONObject json, String key, String fallback) {
    if (json.isNull(key)) return fallback;
    else return json.optString(key, fallback);
  }

  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public static double getDouble(JSONObject object, String key) {
    try {return optDouble(object, key).getAsDouble();}
    catch (NoSuchElementException exception)
    {return throwKeyNotFound(object, key);}
  }

  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  public static OptionalDouble optDouble(JSONObject object, String key) {
    return get(object, key)
      .map(value -> {
        if (value instanceof Double)
          return OptionalDouble.of((Double) value);
        else if (value instanceof Number)
          return OptionalDouble.of(((Number) value).doubleValue());
        else if (value instanceof String)
          try {return OptionalDouble.of(Double.valueOf((String) value));}
          catch (NumberFormatException ignored){return OptionalDouble.empty();}
        else return OptionalDouble.empty();
      }).orElseGet(OptionalDouble::empty);
  }

  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public static int getInteger(JSONObject object, String key) {
    try {return optInteger(object, key).getAsInt();}
    catch (NoSuchElementException exception)
    {return throwKeyNotFound(object, key);}
  }

  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  public static OptionalInt optInteger(JSONObject object, String key) {
    return get(object, key)
      .map(value -> {
        if (value instanceof Integer) return OptionalInt.of((Integer) value);
        else if (value instanceof Number) return OptionalInt.of(((Number) value).intValue());
        else if (value instanceof String)
          try {return OptionalInt.of((int) Double.parseDouble((String) value));}
          catch (NumberFormatException ignored) {return OptionalInt.empty();}
        else return OptionalInt.empty();
      }).orElseGet(OptionalInt::empty);
  }

  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public static long getLong(JSONObject object, String key) {
    try {return optLong(object, key).getAsLong();}
    catch (NoSuchElementException exception)
    {return throwKeyNotFound(object, key);}
  }

  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  public static OptionalLong optLong(JSONObject object, String key) {
    return get(object, key)
      .map(value -> {
        if (value instanceof Long) {
          return OptionalLong.of((Long) value);
        } else if (value instanceof Number) {
          return OptionalLong.of(((Number) value).longValue());
        } else if (value instanceof String)
          try {return OptionalLong.of((long) Double.parseDouble((String) value));}
          catch (NumberFormatException ignored) {return OptionalLong.empty();}
        else return OptionalLong.empty();
      }).orElseGet(OptionalLong::empty);
  }

  /**
   * @param object json object
   * @param key    json key
   *
   * @return optional value
   */
  private static Optional<Object> get(JSONObject object, String key)
  {return ofNullable(object.opt(key)).filter(v -> v != NULL);}

  private static Optional<Object> get(JSONArray array, int index)
  {return ofNullable(array.opt(index)).filter(v -> v != NULL);}

  public static Optional<JSONObject> getJsonObject(JSONObject object, String key) {
    return Optional.ofNullable(object.optJSONObject(key));
  }

  public static Optional<String> getJsonString(JSONObject object, String key) {
    return Optional.ofNullable(object.optString(key));
  }

  /**
   * @param object json object
   * @param key missing key
   */
  @SuppressWarnings("UnusedReturnValue")
  private static <T> T throwKeyNotFound(JSONObject object, String key)
  {throw new IllegalArgumentException('"' + key + '"' + " not found in json:\n" + object);}

  /**
   * @param array json array
   * @param index missing index
   */
  @SuppressWarnings("UnusedReturnValue")
  private static <T> T throwKeyNotFound(JSONArray array, int index)
  {throw new IllegalArgumentException('"' + index + '"' + " not found in json:\n" + array);}

  /** Json builder */
  @FunctionalInterface
  public interface JsonBuilder {
    void construct(JSONObject body) throws JSONException;
  }
}
