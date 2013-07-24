package com.esri.geoevent.transport.twitter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OAuth
{
  private static final Log logger = LogFactory.getLog(OAuth.class);

  public final static class BASE64Encoder
  {
    private static final char   last2byte   = (char) Integer.parseInt("00000011", 2);
    private static final char   last4byte   = (char) Integer.parseInt("00001111", 2);
    private static final char   last6byte   = (char) Integer.parseInt("00111111", 2);
    private static final char   lead6byte   = (char) Integer.parseInt("11111100", 2);
    private static final char   lead4byte   = (char) Integer.parseInt("11110000", 2);
    private static final char   lead2byte   = (char) Integer.parseInt("11000000", 2);
    private static final char[] encodeTable = new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };

    private BASE64Encoder()
    {
    }

    public static String encode(byte[] from)
    {
      StringBuffer to = new StringBuffer((int) (from.length * 1.34) + 3);
      int num = 0;
      char currentByte = 0;
      for (int i = 0; i < from.length; i++)
      {
        num = num % 8;
        while (num < 8)
        {
          switch (num)
          {
            case 0:
              currentByte = (char) (from[i] & lead6byte);
              currentByte = (char) (currentByte >>> 2);
              break;
            case 2:
              currentByte = (char) (from[i] & last6byte);
              break;
            case 4:
              currentByte = (char) (from[i] & last4byte);
              currentByte = (char) (currentByte << 2);
              if ((i + 1) < from.length)
              {
                currentByte |= (from[i + 1] & lead2byte) >>> 6;
              }
              break;
            case 6:
              currentByte = (char) (from[i] & last2byte);
              currentByte = (char) (currentByte << 4);
              if ((i + 1) < from.length)
              {
                currentByte |= (from[i + 1] & lead4byte) >>> 4;
              }
              break;
          }
          to.append(encodeTable[currentByte]);
          num += 6;
        }
      }
      if (to.length() % 4 != 0)
      {
        for (int i = 4 - to.length() % 4; i > 0; i--)
        {
          to.append("=");
        }
      }
      return to.toString();
    }
  }

  public static final String HMACSHAR1              = "HmacSHA1";
  public static final String HMAC_SHA1              = "HMAC-SHA1";
  public static final String OAUTH_CONSUMER_KEY     = "oauth_consumer_key";
  public static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
  public static final String OAUTH_TIMESTAMP        = "oauth_timestamp";
  public static final String OAUTH_NONCE            = "oauth_nonce";
  public static final String OAUTH_VERSION          = "oauth_version";
  public static final String OAUTH_TOKEN            = "oauth_token";
  public static final String OAUTH_SIGNATURE        = "oauth_signature";
  public static final String REALM                  = "realm";
  public static final String OAUTH                  = "OAuth ";
  public static final String AUTHORIZATION          = "Authorization";
  public static final String ACCEPT                 = "Accept";
  public static final String ACCEPT_VALUES          = "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2";
  public static final String CONTENT_TYPE           = "Content-Type";
  public static final String CONSUMER_KEY           = "consumerKey";
  public static final String CONSUMER_SECRET        = "consumerSecret";
  public static final String ACCESS_TOKEN           = "accessToken";
  public static final String ACCESS_TOKEN_SECRET    = "accessTokenSecret";
  public static final String UTF_8                  = "UTF-8";
  private static Random      RAND                   = new Random();

  public static Long getUserIdFromAccessToken(String accessToken)
  {
    // Get userId from accessToken
    Long userId = (long) 0;
    try
    {
      String sUserId;
      sUserId = accessToken.substring(0, accessToken.indexOf("-"));
      if (sUserId != null)
      {
        userId = Long.parseLong(sUserId);
      }
    }
    catch (IndexOutOfBoundsException e)
    {
      throw new IllegalArgumentException("Invalid access token format.");
    }
    return userId;
  }

  public static String encodePostBody(String unEncodedPostBody)
  {
    if (unEncodedPostBody == null)
      unEncodedPostBody = "";

    String[] sParams = unEncodedPostBody.split("&");

    Map<String, String> params = new HashMap<String, String>();
    // params.put("count", "0");
    // params.put("track", "obama");
    for (String param : sParams)
    {
      String[] nameValue = param.split("=");
      if (nameValue.length == 2)
      {
        params.put(nameValue[0], nameValue[1]);
      }
    }
    return encodeParameters(params, "&", false);
  }

  public static String createOAuthAuthorizationHeader(String clientUrl, String httpMethod, String unEncodedPostBody, String accessToken, String accessTokenSecret, String consumerKey, String consumerSecret)
  {
    // String url = "https://stream.twitter.com/1/statuses/filter.json";
    if (unEncodedPostBody == null)
      unEncodedPostBody = "";

    String[] sParams = unEncodedPostBody.split("&");

    Map<String, String> params = new HashMap<String, String>();
    // params.put("count", "0");
    // params.put("track", "esri");
    for (String param : sParams)
    {
      String[] nameValue = param.split("=");
      if (nameValue.length == 2)
      {
        params.put(nameValue[0], nameValue[1]);
      }
    }
    Long ltimestamp = System.currentTimeMillis() / 1000;
    String timestamp = ltimestamp.toString();
    Long lnonce = ltimestamp + OAuth.RAND.nextInt();
    String nonce = lnonce.toString();
    String realm = null;
    String authorizationHeader = generateAuthorizationHeader(httpMethod, clientUrl, params, nonce, timestamp, accessToken, accessTokenSecret, consumerKey, consumerSecret, realm);

    logger.debug(authorizationHeader);
    return authorizationHeader;
  }

  private static void parseGetParameters(String url, Map<String, String> signatureBaseParams)
  {
    int queryStart = url.indexOf("?");
    if (-1 != queryStart)
    {
      String[] queryStrs = url.substring(queryStart + 1).split("&");
      try
      {
        for (String query : queryStrs)
        {
          String[] split = query.split("=");
          if (split.length == 2)
          {
            signatureBaseParams.put(URLDecoder.decode(split[0], OAuth.UTF_8), URLDecoder.decode(split[1], OAuth.UTF_8));
          }
          else
          {
            signatureBaseParams.put(URLDecoder.decode(split[0], OAuth.UTF_8), "");
          }
        }
      }
      catch (UnsupportedEncodingException ignore)
      {
        logger.error(ignore.getMessage(), ignore);
      }
    }
  }

  public static String encode(String value)
  {
    String encoded = null;
    try
    {
      encoded = URLEncoder.encode(value, OAuth.UTF_8);
    }
    catch (UnsupportedEncodingException ignore)
    {
    }
    StringBuffer buf = new StringBuffer(encoded.length());
    char focus;
    for (int i = 0; i < encoded.length(); i++)
    {
      focus = encoded.charAt(i);
      if (focus == '*')
      {
        buf.append("%2A");
      }
      else if (focus == '+')
      {
        buf.append("%20");
      }
      else if (focus == '%' && (i + 1) < encoded.length() && encoded.charAt(i + 1) == '7' && encoded.charAt(i + 2) == 'E')
      {
        buf.append('~');
        i += 2;
      }
      else
      {
        buf.append(focus);
      }
    }
    return buf.toString();
  }

  public static String constructRequestURL(String url)
  {
    int index = url.indexOf("?");
    if (-1 != index)
    {
      url = url.substring(0, index);
    }
    int slashIndex = url.indexOf("/", 8);
    String baseURL = url.substring(0, slashIndex).toLowerCase();
    int colonIndex = baseURL.indexOf(":", 8);
    if (-1 != colonIndex)
    {
      // url contains port number
      if (baseURL.startsWith("http://") && baseURL.endsWith(":80"))
      {
        // http default port 80 MUST be excluded
        baseURL = baseURL.substring(0, colonIndex);
      }
      else if (baseURL.startsWith("https://") && baseURL.endsWith(":443"))
      {
        // http default port 443 MUST be excluded
        baseURL = baseURL.substring(0, colonIndex);
      }
    }
    url = baseURL + url.substring(slashIndex);

    return url;
  }

  public static String encodeParameters(Map<String, String> httpParams, String splitter, boolean quot)
  {
    StringBuffer buf = new StringBuffer();
    Set<String> keys = httpParams.keySet();
    List<String> keyList = new ArrayList<String>();
    for (Object key : keys)
    {
      keyList.add((String) key);
    }
    Collections.sort(keyList);
    for (String key : keyList)
    {
      // if (!param.isFile())
      {
        if (buf.length() != 0)
        {
          if (quot)
          {
            buf.append("\"");
          }
          buf.append(splitter);
        }
        buf.append(encode(key)).append("=");
        if (quot)
        {
          buf.append("\"");
        }
        buf.append(encode(httpParams.get(key)));
      }
    }
    if (buf.length() != 0)
    {
      if (quot)
      {
        buf.append("\"");
      }
    }
    return buf.toString();
  }

  public static String generateSignature(String oauthBaseString, String accessToken, String accessTokenSecret, String consumerSecret, SecretKeySpec secretKeySpec)
  {
    byte[] byteHMAC = null;
    try
    {
      Mac mac = Mac.getInstance(OAuth.HMACSHAR1);
      SecretKeySpec spec;
      if (null == accessToken)
      {
        String oauthSignature = encode(consumerSecret) + "&";
        spec = new SecretKeySpec(oauthSignature.getBytes(), OAuth.HMACSHAR1);
      }
      else
      {
        spec = secretKeySpec;
        if (null == spec)
        {
          String oauthSignature = encode(consumerSecret) + "&" + encode(accessTokenSecret);
          spec = new SecretKeySpec(oauthSignature.getBytes(), OAuth.HMACSHAR1);

          // TODO -- out param!
          secretKeySpec = spec;
        }
      }
      mac.init(spec);
      byteHMAC = mac.doFinal(oauthBaseString.getBytes());
    }
    catch (InvalidKeyException ike)
    {
      // logger.error("Failed initialize \"Message Authentication Code\" (MAC)",
      // ike);
      throw new AssertionError(ike);
    }
    catch (NoSuchAlgorithmException nsae)
    {
      // logger.error("Failed to get HmacSHA1 \"Message Authentication Code\" (MAC)",
      // nsae);
      throw new AssertionError(nsae);
    }
    return OAuth.BASE64Encoder.encode(byteHMAC);
  }

  public static String generateAuthorizationHeader(String method, String url, Map<String, String> params, String nonce, String timestamp, String token, String tokenSecret, String consumerKey, String consumerSecret, String realm)
  {
    Map<String, String> headerParams = new TreeMap<String, String>();
    headerParams.put(OAuth.OAUTH_CONSUMER_KEY, consumerKey);
    headerParams.put(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
    headerParams.put(OAuth.OAUTH_TIMESTAMP, timestamp);
    headerParams.put(OAuth.OAUTH_NONCE, nonce);
    headerParams.put(OAuth.OAUTH_VERSION, "1.0");
    headerParams.put(OAuth.OAUTH_TOKEN, token);

    Map<String, String> signatureParams = new HashMap<String, String>();
    signatureParams.putAll(headerParams);
    // if !containsFile
    if (params != null)
      signatureParams.putAll(params);

    parseGetParameters(url, signatureParams);

    StringBuffer base = new StringBuffer(method).append("&").append(encode(constructRequestURL(url))).append("&");
    base.append(encode(encodeParameters(signatureParams, "&", false)));
    String oauthBaseString = base.toString();
    // logger.debug("OAuth base string: ", oauthBaseString);
    SecretKeySpec secretKeySpec = null;
    String signature = generateSignature(oauthBaseString, token, tokenSecret, consumerSecret, secretKeySpec);
    // logger.debug("OAuth signature: ", signature);

    headerParams.put(OAuth.OAUTH_SIGNATURE, signature);

    // http://oauth.net/core/1.0/#rfc.section.9.1.1
    if (realm != null)
    {
      headerParams.put(OAuth.REALM, realm);
    }
    return OAuth.OAUTH + encodeParameters(headerParams, ",", true);
  }

}
