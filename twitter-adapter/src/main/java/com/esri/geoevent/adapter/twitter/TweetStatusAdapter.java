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

package com.esri.geoevent.adapter.twitter;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import com.esri.geoevent.adapter.twitter.Tweet.BoundingBox;
import com.esri.geoevent.adapter.twitter.Tweet.Coordinates;
import com.esri.geoevent.adapter.twitter.Tweet.Place;
import com.esri.geoevent.adapter.twitter.Tweet.User;
import com.esri.ges.adapter.AdapterDefinition;
import com.esri.ges.adapter.InboundAdapterBase;
import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.geoevent.FieldGroup;
import com.esri.ges.core.geoevent.GeoEvent;
import com.esri.ges.core.geoevent.GeoEventDefinition;
import com.esri.ges.messaging.MessagingException;
import com.esri.ges.spatial.Point;

public class TweetStatusAdapter extends InboundAdapterBase
{
  private static final Log LOG    = LogFactory.getLog(TweetStatusAdapter.class);
  private ObjectMapper     mapper = new ObjectMapper();
  private SimpleDateFormat sdf    = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
  private Charset          charset;
  private CharsetDecoder   decoder;

  public TweetStatusAdapter(AdapterDefinition definition) throws ComponentException
  {
    super(definition);
    LOG.debug("Tweet Status Adapter created");
    charset = Charset.forName("UTF-8");
    decoder = charset.newDecoder();
  }

  private class TweetEventBuilder implements Runnable
  {
    private StringBuilder sb;

    TweetEventBuilder(String text)
    {
      this.sb = new StringBuilder(text);
    }

    private GeoEvent buildGeoEvent() throws Exception
    {
      // 3 lines below are not necessary I think. These were added when I
      // was having problems with encoding
      // //byte[] strBytes = sb.toString().getBytes("UTF-8");
      // //String s = new String(strBytes,"UTF-8");
      // //Tweet jsonTweet = mapper.readValue(s, Tweet.class);

      // atempt to parse string to Tweet
      Tweet jsonTweet = mapper.readValue(sb.toString(), Tweet.class);
      // consoleDebugPrintLn(sb.toString());
      if (jsonTweet == null)
      {

        consoleDebugPrintLn("jsonTweet is null");
        return null;
      }
      // consoleDebugPrintLn(jsonTweet.getText());

      // Create an instance of the message using the guid that we
      // generated when we started up.
      GeoEvent msg;
      try
      {
        AdapterDefinition def = (AdapterDefinition) definition;
        // XmlAdapterDefinition tweetDef = (XmlAdapterDefinition) definition;
        // String name = tweetDef.getName();
        GeoEventDefinition geoDef = def.getGeoEventDefinition("TweetStatus");
        if (geoEventCreator.getGeoEventDefinitionManager().searchGeoEventDefinition(geoDef.getName(), geoDef.getOwner()) == null)
        {
          geoEventCreator.getGeoEventDefinitionManager().addGeoEventDefinition(geoDef);
        }
        msg = geoEventCreator.create(geoDef.getName(), geoDef.getOwner());
        LOG.debug("Created new TweetStatusMessage");
      }
      catch (MessagingException e)
      {
        LOG.error("Message Creation error in TweetStatusAdapter: " + e.getMessage());
        return null;
      }
      catch (Exception ex)
      {
        LOG.error("Error creating initial tweet message: " + ex.getMessage());
        return null;
      }

      // Populate the message with all the attribute values.
      // first is geometry - get from tweet coordinates or place
      double x = Double.NaN;
      double y = Double.NaN;
      int wkid = 4326;
      Coordinates coords = jsonTweet.getCoordinates();
      Place place = jsonTweet.getPlace();
      User user = jsonTweet.getUser();
      if (coords != null)
      {
        x = coords.getCoordinates().get(0);
        y = coords.getCoordinates().get(1);
      }
      if (place != null)
      {
        // if still need coordinates0..

        // get bounding box of place and figure center
        if (Double.isNaN(x) && Double.isNaN(y))
        {
          BoundingBox bbox = place.getBounding_box();
          if (bbox != null)
          {
            ArrayList<Double> ll = bbox.getCoordinates().get(0).get(0);
            ArrayList<Double> ur = bbox.getCoordinates().get(0).get(2);
            Double xmin = ll.get(0);
            Double xmax = ur.get(0);
            Double ymin = ll.get(1);
            Double ymax = ur.get(1);
            x = xmin + ((xmax - xmin) / 2);
            y = ymin + ((ymax - ymin) / 2);
          }
        }
        // set attributes in message associated with place
        FieldGroup placeGrp = msg.createFieldGroup("place");
        placeGrp.setField(0, place.getId());
        placeGrp.setField(1, place.getFull_name());
        placeGrp.setField(2, place.getUrl());
        msg.setField(13, placeGrp);
      }

      // set geometry in message if an xy coordinate was found
      // and set geolocated attribute to true or false
      if (!Double.isNaN(x) && !Double.isNaN(y))
      {
        Point pt = spatial.createPoint(x, y, wkid);
        msg.setField(5, pt);
        msg.setField(15, true);
        LOG.debug("Generated tweet with location");
        // consoleDebugPrintLn("tweet with location");
      }
      else
      {
        msg.setField(15, false);
      }

      // set rest of attributes in message
      msg.setField(0, jsonTweet.getPossibly_sensitive_editable());
      msg.setField(1, jsonTweet.getText());
      String createdAt = jsonTweet.getCreated_at();
      try
      {
        if (createdAt != null)
          msg.setField(2, sdf.parse(jsonTweet.getCreated_at()));
      }
      catch (Exception e)
      {
        LOG.warn("Parse date exception in TweetStatusAdapter: " + e.getMessage());
        LOG.debug(e.getMessage(), e);
      }
      msg.setField(3, jsonTweet.getRetweeted());
      msg.setField(4, jsonTweet.getRetweet_count());
      msg.setField(6, jsonTweet.getId_str());
      msg.setField(7, jsonTweet.getIn_reply_to_screen_name());
      msg.setField(8, jsonTweet.getIn_reply_to_status_id_str());
      msg.setField(9, jsonTweet.getFavorited());
      msg.setField(10, jsonTweet.getTruncated());
      msg.setField(11, jsonTweet.getPossibly_sensitive());
      msg.setField(12, jsonTweet.getIn_reply_to_user_id_str());
      if (user != null)
      {
        FieldGroup userGrp = msg.createFieldGroup("user");
        userGrp.setField(0, user.getId_str());
        userGrp.setField(1, user.getName());
        userGrp.setField(2, user.getFollowers_count());
        userGrp.setField(3, user.getLocation());
        userGrp.setField(4, user.getScreen_name());
        msg.setField(14, userGrp);
      }

      return msg;
    }

    @Override
    public void run()
    {
      try
      {
        GeoEvent event = buildGeoEvent();
        if (event != null)
        {
          geoEventListener.receive(event);
        }
      }
      catch (Throwable t)
      {
        LOG.error("Unexpected error", t);
      }
    }

  }

  @Override
  public void receive(ByteBuffer buffer, String channelId)
  {
    if (!buffer.hasRemaining())
      return;

    try
    {
      CharBuffer charBuffer = decoder.decode(buffer);
      String text = charBuffer.toString();
      TweetEventBuilder builder = new TweetEventBuilder(text);
      Thread t = new Thread(builder);
      t.setName("Twitter Event Builder " + System.identityHashCode(buffer));
      t.start();
    }
    catch (CharacterCodingException e)
    {
      LOG.warn("Could not decode the incoming buffer - " + e);
      buffer.clear();
      return;
    }
  }

  @Override
  protected GeoEvent adapt(ByteBuffer buffer, String channelId)
  {
    return null;
  }

  public static void consoleDebugPrintLn(String msg)
  {
    String consoleOut = System.getenv("GEP_CONSOLE_OUTPUT");
    if (consoleOut != null && "1".equals(consoleOut))
    {
      System.out.println(msg);
      LOG.debug(msg);
    }
  }

  public static void consoleDebugPrint(String msg)
  {
    String consoleOut = System.getenv("GEP_CONSOLE_OUTPUT");
    if (consoleOut != null && "1".equals(consoleOut))
    {
      System.out.print(msg);
      LOG.debug(msg);
    }
  }
}
