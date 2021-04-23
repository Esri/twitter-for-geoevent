/*
  Copyright 1995-2013 Esri

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

  For additional information, contact:
  Environmental Systems Research Institute, Inc.
  Attn: Contracts Dept
  380 New York Street
  Redlands, California, USA 92373

  email: contracts@esri.com
 */

package com.esri.geoevent.transport.twitter;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpRequest;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.geoevent.GeoEvent;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.TransportContext;
import com.esri.ges.transport.TransportDefinition;
import com.esri.ges.transport.http.HttpOutboundTransport;
import com.esri.ges.transport.http.HttpTransportContext;

public class TwitterOutboundTransport extends HttpOutboundTransport
{
  private static final BundleLogger LOGGER = BundleLoggerFactory.getLogger(TwitterOutboundTransport.class);

  private String                    consumerKey;
  private String                    consumerSecret;
  private String                    accessToken;
  private String                    accessTokenSecret;
  private String                    postBodyOrg;
  private CharsetDecoder            decoder;

  public TwitterOutboundTransport(TransportDefinition definition) throws ComponentException
  {
    super(definition);
    decoder = createCharsetDecoder();
  }

  @Override
  public synchronized void start()
  {
    super.start();
    LOGGER.debug("OUTBOUND_START");
  }

  @Override
  public synchronized void stop()
  {
    super.stop();
    LOGGER.debug("OUTBOUND_STOP");
  }

  @Override
  public synchronized void setup()
  {
    super.setup();
    try
    {
      applyProperties();
    }
    catch (Exception error)
    {
      LOGGER.error("OUTBOUND_TRANSPORT_SETUP_ERROR", error.getMessage());
      LOGGER.info(error.getMessage(), error);
    }
  }

  @Override
  public void receive(ByteBuffer bb, String channelId, GeoEvent geoEvent)
  {
    receive(bb, channelId);
  }

  @Override
  public void receive(ByteBuffer bb, String channelId)
  {
    try
    {
      postBodyOrg = "status=" + decoder.decode(bb).toString();
      postBody = OAuth.encodePostBody(postBodyOrg);
      LOGGER.debug(postBody);
      doHttp(clientUrl);
    }
    catch (Exception e)
    {
      LOGGER.error("ERROR_SENDING_MSG", e);
    }
  }

  @Override
  public void beforeConnect(TransportContext context)
  {
    // String url = "https://api.twitter.com/1.1/statuses/update.json";
    HttpRequest request = ((HttpTransportContext) context).getHttpRequest();

    String authorizationHeader = OAuth.createOAuthAuthorizationHeader(clientUrl, httpMethod, postBodyOrg, accessToken, accessTokenSecret, consumerKey, consumerSecret);

    // log.debug(authorizationHeader);
    request.addHeader(OAuth.AUTHORIZATION, authorizationHeader);
    request.addHeader(OAuth.ACCEPT, OAuth.ACCEPT_VALUES);// "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"
    request.setHeader(OAuth.CONTENT_TYPE, this.postBodyType);// "application/x-www-form-urlencoded"
  }

  @Override
  public void validate()
  {
    LOGGER.debug("OUTBOUND_SKIP_VALIDATION");
  }

  public void applyProperties() throws Exception
  {
    if (getProperty(OAuth.CONSUMER_KEY).isValid())
    {
      String value = (String) getProperty(OAuth.CONSUMER_KEY).getValue();
      if (value.length() > 0)
      {
        consumerKey = cryptoService.decrypt(value);
      }
    }
    if (getProperty(OAuth.CONSUMER_SECRET).isValid())
    {
      String value = (String) getProperty(OAuth.CONSUMER_SECRET).getValue();
      if (value.length() > 0)
      {
        consumerSecret = cryptoService.decrypt(value);
      }
    }
    if (getProperty(OAuth.ACCESS_TOKEN).isValid())
    {
      String value = (String) getProperty(OAuth.ACCESS_TOKEN).getValue();
      if (value.length() > 0)
      {
        accessToken = cryptoService.decrypt(value);
      }
    }
    if (getProperty(OAuth.ACCESS_TOKEN_SECRET).isValid())
    {
      String value = (String) getProperty(OAuth.ACCESS_TOKEN_SECRET).getValue();
      if (value.length() > 0)
      {
        accessTokenSecret = cryptoService.decrypt(value);
      }
    }
  }

  private CharsetDecoder createCharsetDecoder()
  {
    Charset charset = StandardCharsets.UTF_8;
    CharsetDecoder decoder = charset.newDecoder();
    decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
    return decoder;
  }
}
