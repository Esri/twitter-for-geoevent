package com.esri.geoevent.transport.twitter;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequest;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.transport.TransportContext;
import com.esri.ges.transport.TransportDefinition;
import com.esri.ges.transport.http.HttpOutboundTransport;
import com.esri.ges.transport.http.HttpTransportContext;

public class TwitterOutboundTransport extends HttpOutboundTransport
{
  static final private Log log = LogFactory.getLog(TwitterOutboundTransport.class);

  private String           consumerKey;
  private String           consumerSecret;
  private String           accessToken;
  private String           accessTokenSecret;
  private String           postBodyOrg;

  public TwitterOutboundTransport(TransportDefinition definition) throws ComponentException
  {
    super(definition);
  }

  @Override
  public synchronized void start()
  {
    super.start();
    log.debug("Http-Oauth-Outbound started.");
  }

  @Override
  public synchronized void stop()
  {
    super.stop();
    log.debug("Http-Oauth-Outbound stopped.");
  }

  @Override
  public synchronized void setup()
  {
    super.setup();
    try
    {
      applyProperties();
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public void receive(ByteBuffer bb, String channelId)
  {
    byte[] data = new byte[bb.remaining()];
    bb.get(data);

    postBodyOrg = new String(data);
    postBody = OAuth.encodePostBody("status=" + postBodyOrg);
    log.debug(postBody);

    // super.receive(bb, channelId);
    doHttp();

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
    log.debug("Outbound Skip validation...");
  }

  public void applyProperties() throws Exception
  {
    if (getProperty(OAuth.CONSUMER_KEY).isValid())
    {
      String value = (String) getProperty(OAuth.CONSUMER_KEY).getValue();
      if (value.length() > 0)
      {
        consumerKey = value;
      }
    }
    if (getProperty(OAuth.CONSUMER_SECRET).isValid())
    {
      String value = (String) getProperty(OAuth.CONSUMER_SECRET).getValue();
      if (value.length() > 0)
      {
        consumerSecret = value;
      }
    }
    if (getProperty(OAuth.ACCESS_TOKEN).isValid())
    {
      String value = (String) getProperty(OAuth.ACCESS_TOKEN).getValue();
      if (value.length() > 0)
      {
        accessToken = value;
      }
    }
    if (getProperty(OAuth.ACCESS_TOKEN_SECRET).isValid())
    {
      String value = (String) getProperty(OAuth.ACCESS_TOKEN_SECRET).getValue();
      if (value.length() > 0)
      {
        accessTokenSecret = value;
      }
    }
  }

}
