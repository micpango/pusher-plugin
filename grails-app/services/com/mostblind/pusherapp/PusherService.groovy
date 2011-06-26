package com.mostblind.pusherapp

import groovyx.net.http.RESTClient
import java.security.InvalidKeyException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import static groovyx.net.http.ContentType.URLENC
import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * Class to send messages to Pusher's REST API.
 *
 * Please set pusherApplicationId, pusherApplicationKey, pusherApplicationSecret accordingly
 * before sending any request.
 *
 * @author Michael Pangopoulos (based on http://tinyurl.com/2urje4s)
 * Copyright 2010. Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 */
class PusherService {

  def config = ConfigurationHolder.config

  def pusherHost = config.pusherapp.host
  def pusherApplicationId = config.pusherapp.applicationId
  def pusherApplicationKey = config.pusherapp.applicationKey
  def pusherApplicationSecret = config.pusherapp.applicationSecret

  private def byteArrayToString(byte[] data) {
    BigInteger bigInteger = new BigInteger(1, data)
    String hash = bigInteger.toString(16)
    // Zero pad it
    while (hash.length() < 64) {
      hash = "0" + hash
    }
    return hash
  }

  /**
   * Returns a HMAC/SHA256 representation of the given string
   * @param data
   * @return
   */
  private def hmacsha256Representation(String data) {
    try {
      final SecretKeySpec signingKey = new SecretKeySpec(pusherApplicationSecret.getBytes(), "HmacSHA256")

      final Mac mac = Mac.getInstance("HmacSHA256")
      mac.init(signingKey)

      byte[] digest = mac.doFinal(data.getBytes("UTF-8"))
      return byteArrayToString(digest)
    } catch (InvalidKeyException e) {
      throw new RuntimeException("Invalid key exception while converting to HMac SHA256")
    }
  }

  /**
   * Build query string that will be appended to the URI and HMAC/SHA256 encoded
   * @param eventName
   * @param jsonData
   * @param socketID
   * @return
   */
  private def buildQuery(String eventName, def jsonData, String socketID) {
    StringBuffer buffer = new StringBuffer()
    buffer.append("auth_key=")
    buffer.append(pusherApplicationKey)
    buffer.append("&auth_timestamp=")
    buffer.append(System.currentTimeMillis() / 1000)
    buffer.append("&auth_version=1.0")
    buffer.append("&body_md5=")
    buffer.append(jsonData.encodeAsMD5())
    buffer.append("&name=")
    buffer.append(eventName)
    if (!socketID.isEmpty()) {
      buffer.append("&socket_id=")
      buffer.append(socketID)
    }
    return buffer.toString()
  }

  /**
   * Build path of the URI that is also required for Authentication
   * @param channelName
   * @return
   */
  private buildURIPath(String channelName) {
    StringBuffer buffer = new StringBuffer()
    //Application ID
    buffer.append("/apps/")
    buffer.append(pusherApplicationId)
    //Channel name
    buffer.append("/channels/")
    buffer.append(channelName)
    //Event
    buffer.append("/events")
    //Return content of buffer
    return buffer.toString()
  }

  /**
   * Build authentication signature to assure that our event is recognized by Pusher
   * @param uriPath
   * @param query
   * @return
   */
  private def buildAuthenticationSignature(String uriPath, String query) {
    StringBuffer buffer = new StringBuffer()
    buffer.append("POST\n")
    buffer.append(uriPath)
    buffer.append("\n")
    buffer.append(query)
    String h = buffer.toString()
    return hmacsha256Representation(h)
  }

  /**
   * Build URI where request is send to
   * @param uriPath
   * @param query
   * @param signature
   * @return
   */
  private def buildURI(String uriPath, String query, String signature) {
    StringBuffer buffer = new StringBuffer()
    buffer.append("http://")
    buffer.append(pusherHost)
    buffer.append(uriPath)
    buffer.append("?")
    buffer.append(query)
    buffer.append("&auth_signature=")
    buffer.append(signature)
    return buffer.toString()
  }

  /**
   * Delivers a message to the Pusher API without providing a socket_id
   * @param channel
   * @param event
   * @param jsonData
   * @return HttpResponse status
   */
  def triggerPush(String channel, String event, String jsonData) {
    triggerPush(channel, event, jsonData, "")
  }

  /**
   * Delivers a message to the Pusher API
   * @param channel
   * @param event
   * @param jsonData
   * @param socketId
   * @return HttpResponse status
   */
  def triggerPush(String channel, String event, String jsonData, String socketId) {

    def uriPath = buildURIPath(channel)
    def query = buildQuery(event, jsonData, socketId)
    def signature = buildAuthenticationSignature(uriPath, query)
    def uri = buildURI(uriPath, query, signature)

    def pusher = new RESTClient(uri)

    try {
      def response = pusher.post(
        requestContentType: URLENC,
        body: jsonData)
      response.status
    } catch (Exception e) {
      log.error("Pusher request failed!")
      null
    }
  }   

  /**
   * Generate the authorization string required for private channels
   * @param socketId
   * @param channel
   * @return String signed code
   */
  def genAuthString(String socketId, String channel) {
    def authToken = socketId + ':' + channel
    pusherApplicationKey + ':' + hmacsha256Representation(authToken)
  }

}
