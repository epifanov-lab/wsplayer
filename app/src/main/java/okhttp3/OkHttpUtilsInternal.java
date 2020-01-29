/*
 * OkHttpUtilsInternal.java
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

package okhttp3;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import okhttp3.MultipartBody.Part;
import okio.ByteString;

import static okhttp3.Protocol.HTTP_1_1;
import static okhttp3.Protocol.HTTP_2;
import static okhttp3.internal.Util.immutableList;


/**
 * @author Nikitenko Gleb
 * @since 1.0, 19/08/2019
 */
public final class OkHttpUtilsInternal {


  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private OkHttpUtilsInternal() {
    throw new AssertionError();
  }

  /**
   * @param client okHttp client
   * @param check wss checker
   *
   * @return call factory
   */
  public static Call.Factory factory(OkHttpClient.Builder client, Predicate<HttpUrl> check) {
    final OkHttpClient
      rest = client.protocols(immutableList(HTTP_2, HTTP_1_1)).build(),
      wss = client.protocols(immutableList(HTTP_1_1)).build();
    return request -> {
      final boolean isWebSocket = check.test(request.url);
      return RealCall.newRealCall(isWebSocket ? wss : rest, request, isWebSocket);
    };
  }

  /**
   * @param boundary multipart boundary
   * @param type     content type
   * @param parts    body parts
   *
   * @return  request body
   */
  public static RequestBody multipart(ByteString boundary, MediaType type, Part... parts)
  {return new MultipartBody(boundary, type, Arrays.asList(parts));}

  /**
   * @param builder http url builder
   * @param query list of query parameters
   */
  public static void query(HttpUrl.Builder builder, List<String> query)
  {builder.encodedQueryNamesAndValues = query;}

  /**
   * @param input input content
   * @param pos position of start
   * @param limit size of input
   * @param set encode set
   * @param encoded true to leave '%' as-is; false to convert it to '%25'.
   * @param strict true to encode '%' if it is not the prefix of a valid percent encoding.
   * @param plusIsSpace true to encode '+' as "%2B" if it is not already encoded.
   * @param asciiOnly true to encode all non-ASCII codePoints.
   * @param charset which charset to use, null equals UTF-8.
   *
   * @return substring of {@code input} on the range {@code [pos..limit)}
   * with the following transformations:
   *  <ul>
   *    <li>Tabs, newlines, form feeds and carriage returns are skipped.
   *    <li>In queries, ' ' is encoded to '+' and '+' is encoded to "%2B".
   *    <li>Characters in {@code encodeSet} are percent-encoded.
   *    <li>Control characters and non-ASCII characters are percent-encoded.
   *    <li>All other characters are copied without transformation.
   *  </ul>
   */
  public static String canonicalize(String input, int pos, int limit, String set,
                                    boolean encoded, boolean strict, boolean plusIsSpace, boolean asciiOnly, Charset charset)
  {return HttpUrl.canonicalize(input, pos, limit, set, encoded, strict, plusIsSpace, asciiOnly, charset);}

}
