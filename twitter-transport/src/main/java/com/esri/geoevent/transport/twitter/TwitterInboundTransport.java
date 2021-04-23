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

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.component.RunningState;
import com.esri.ges.core.validation.ValidationException;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.InboundTransportBase;
import com.esri.ges.transport.TransportDefinition;
import com.esri.ges.util.Validator;

import twitter4j.ConnectionLifeCycleListener;
import twitter4j.FilterQuery;
import twitter4j.RawStreamListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterInboundTransport extends InboundTransportBase implements Runnable
{
  public TwitterInboundTransport(TransportDefinition definition) throws ComponentException
  {
    super(definition);
  }

  static final private BundleLogger LOGGER    = BundleLoggerFactory.getLogger(TwitterInboundTransport.class);

  private String                    consumerKey;
  private String                    consumerSecret;
  private String                    accessToken;
  private String                    accessTokenSecret;

  private long[]                    follows   = null;
  private String[]                  tracks    = null;
  private double[][]                locations = null;
  private int                       count     = -1;

  private String                    filterString;
  private TwitterStream             twitterStream;
  private Thread                    thread    = null;

  private final TwitterConnectionLifeCycleListener connectionListener = new TwitterConnectionLifeCycleListener();

  @SuppressWarnings("incomplete-switch")
  @Override
  public synchronized void start()
  {
    try
    {
      switch (getRunningState())
      {
        case STARTING:
        case STARTED:
        case STOPPING:
          return;
      }
      setRunningState(RunningState.STARTING);
      thread = new Thread(this);
      thread.start();
    }
    catch (Exception e)
    {
      LOGGER.error("UNEXPECTED_ERROR_STARTING", e);
      stop();
    }
  }

  @Override
  public synchronized void stop()
  {
    try
    {
      if (this.twitterStream != null)
      {
        twitterStream.cleanUp();
        twitterStream.shutdown();
      }
    }
    catch (Exception ex)
    {
      LOGGER.error("UNABLE_TO_CLOSE", ex);
    }
    setRunningState(RunningState.STOPPED);
    LOGGER.debug("INBOUND_STOP");
  }

  @Override
  public void validate() throws ValidationException
  {
    try
    {
      applyProperties();
    }
    catch (Exception e)
    {
      ValidationException validex = new ValidationException(e.getLocalizedMessage());
      validex.addSuppressed(e);
      throw validex;
    }
    // LOGGER.debug("INBOUND_SKIP_VALIDATION");
    String keywords[] = tracks;
    if (follows != null && follows.length > 0)
      ;// ok
    else if (keywords != null && keywords.length > 0)
      ;// ok
    else if (locations != null)
      ;// ok
    else
      throw new ValidationException(LOGGER.translate("INBOUND_TRANSPORT_NOFILTER_ERROR"));
  }

  public void applyProperties() throws Exception
  {
    consumerKey = null;
    consumerSecret = null;
    accessToken = null;
    accessTokenSecret = null;
    follows = null;
    tracks = null;
    locations = null;
    count = -1;

    if (getProperty(OAuth.CONSUMER_KEY).isValid())
    {
      String value = (String) getProperty(OAuth.CONSUMER_KEY).getValue();
      if (value != null)
      {
      if (value.length() > 0)
      {
        consumerKey = cryptoService.decrypt(value);
      }
    }
    }

    if (getProperty(OAuth.CONSUMER_SECRET).isValid())
    {
      String value = (String) getProperty(OAuth.CONSUMER_SECRET).getValue();
      if (value != null)
      {
      if (value.length() > 0)
      {
        consumerSecret = cryptoService.decrypt(value);
      }
    }
    }

    if (getProperty(OAuth.ACCESS_TOKEN).isValid())
    {
      String value = (String) getProperty(OAuth.ACCESS_TOKEN).getValue();
      if (value != null)
      {
      if (value.length() > 0)
      {
        accessToken = cryptoService.decrypt(value);
      }
    }
    }

    if (getProperty(OAuth.ACCESS_TOKEN_SECRET).isValid())
    {
      String value = (String) getProperty(OAuth.ACCESS_TOKEN_SECRET).getValue();
      if (value != null)
      {
      if (value.length() > 0)
      {
        accessTokenSecret = cryptoService.decrypt(value);
      }
    }
    }

    StringBuilder paramsStr = new StringBuilder();
    if (getProperty("follow").isValid())
    {
      String value = (String) getProperty("follow").getValue();
      if (value != null)
      {
      if (StringUtils.isNotEmpty(value))
      {
        paramsStr.append("follow=" + value);
        String[] flwStrs = value.split(",");
        if (flwStrs.length > 0)
        {
            ArrayList<Long> followsList = new ArrayList<Long>();

          for (int i = 0; i < flwStrs.length; i++)
          {
              if (flwStrs[i] != null && !flwStrs[i].trim().isEmpty())
              {
                try
                {
                  followsList.add(Long.parseLong(flwStrs[i].trim()));
                }
                catch (NumberFormatException e)
                {
                  LOGGER.warn("Failed to set follow ID: {}", e, flwStrs[i].trim());
                }
              }
            }

            if (followsList.size() > 0)
            {
              follows = new long[followsList.size()];
              for (int index = 0; index < followsList.size(); index++)
              {
                follows[index] = followsList.get(index);
              }
            }
          }
        }
      }
    }
    if (getProperty("track").isValid())
    {
      String value = (String) getProperty("track").getValue();
      if (value != null)
      {
      if (value.length() > 0)
      {
        if (paramsStr.length() > 0)
        {
          paramsStr.append("&");
        }
        paramsStr.append("track=" + value);

        tracks = value.split(",");
        for (int i = 0; i < tracks.length; i++)
        {
          tracks[i] = tracks[i].trim();
        }
      }
    }
    }
    if (getProperty("locations").isValid())
    {
      String value = (String) getProperty("locations").getValue();
      if (value != null)
      {
      if (value.length() > 0)
      {
        if (paramsStr.length() > 0)
        {
          paramsStr.append("&");
        }
        paramsStr.append("locations=" + value);

        String[] crdStrs = value.split(",");
        int length = crdStrs.length;
        // lengh should be multiple of 4
        if (length % 4 == 0)
        {
          int dimension = length / 2;
          locations = new double[dimension][2];
          for (int i = 0; i < dimension; i++)
          {
            for (int j = 0; j < 2; j++)
            {
              locations[i][j] = Double.parseDouble(crdStrs[i * 2 + j].trim());
            }
          }
        }
      }
    }
    }
    // required elevated access to use
    if (getProperty("count").isValid())
    {
      Object prop = getProperty("count").getValue();
      if (prop != null)
      {
      count = (Integer) prop;
      if (count < -150000 || count > 150000)
      {
        LOGGER.error("INBOUND_COUNT_VALIDATION");
      }
      else
      {
        if (paramsStr.length() > 0)
        {
          paramsStr.append("&");
        }
        paramsStr.append("count=" + prop.toString());
      }
    }
    }
    if (paramsStr.length() > 0)
    {
      filterString = paramsStr.toString();
    }
  }

  @Override
  public void run()
  {
    receiveData();

  }

  private void receiveData()
  {
    try
    {
      applyProperties();
      setErrorMessage("");
      setRunningState(RunningState.STARTED);

      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.setDebugEnabled(true);
      cb.setOAuthConsumerKey(consumerKey);
      cb.setOAuthConsumerSecret(consumerSecret);
      cb.setOAuthAccessToken(accessToken);
      cb.setOAuthAccessTokenSecret(accessTokenSecret);
      twitterStream = new TwitterStreamFactory(cb.build()).getInstance();

      RawStreamListener rawStreamListener = new RawStreamListener()
        {

          @Override
          public void onException(Exception ex)
          {
            LOGGER.error("INBOUND_TRANSPORT_RAW_STREAM_LISTERNER_EXCEPTION", ex.getMessage());
          }

          @Override
          public void onMessage(String rawString)
          {
            LOGGER.trace("Received Message String: {0}", rawString);
            receive(rawString);
          }
        };

      FilterQuery fq = new FilterQuery();

      String keywords[] = tracks;

      if (follows != null && follows.length > 0)
        fq.follow(follows);
      else if (keywords != null && keywords.length > 0)
        fq.track(keywords);
      else if (locations != null)
        fq.locations(locations);
      else
        throw new Exception(LOGGER.translate("INBOUND_TRANSPORT_NOFILTER_ERROR"));

      fq.count(count);

      LOGGER.info("INBOUND_TRANSPORT_FILTER", filterString);

      twitterStream.addListener(rawStreamListener);
      twitterStream.addConnectionLifeCycleListener(connectionListener);
      twitterStream.filter(fq);

    }
    catch (Throwable ex)
    {
      LOGGER.error("UNEXPECTED_ERROR", ex);
      setErrorMessage(ex.getLocalizedMessage());
      setRunningState(RunningState.ERROR);
    }
  }

  private class TwitterConnectionLifeCycleListener implements ConnectionLifeCycleListener
  {
    @Override
    public void onDisconnect()
    {
      LOGGER.trace("Disconnect");
    }

    @Override
    public void onConnect()
    {
      LOGGER.trace("Connected");
    }

    @Override
    public void onCleanUp()
    {
      LOGGER.trace("Cleanup");
    }
  }

  private void receive(String tweet)
  {
    if (!Validator.isEmpty(tweet))
    {
      byte[] newBytes = tweet.getBytes();

      ByteBuffer bb = ByteBuffer.allocate(newBytes.length);
      try
      {
        bb.put(newBytes);
        bb.flip();
        byteListener.receive(bb, "");
        bb.clear();
      }
      catch (BufferOverflowException boe)
      {
        LOGGER.error("BUFFER_OVERFLOW_ERROR", boe);
        bb.clear();
        setRunningState(RunningState.ERROR);
      }
      catch (Exception e)
      {
        LOGGER.error("UNEXPECTED_ERROR2", e);
        stop();
        setRunningState(RunningState.ERROR);
      }
    }
  }
}
