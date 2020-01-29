/*
 * OkioUtilsInternal.java
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

package okio;

public class OkioUtilsInternal {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private OkioUtilsInternal() {
    throw new AssertionError();
  }

  /**
   * @param value byte string
   *
   * @return byte array
   */
  public static byte[] bytes(ByteString value)
  {return value.data;}


}
