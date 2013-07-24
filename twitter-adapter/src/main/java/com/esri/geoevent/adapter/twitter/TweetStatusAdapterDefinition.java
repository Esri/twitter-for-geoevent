package com.esri.geoevent.adapter.twitter;

import java.util.ArrayList;
import java.util.List;

import com.esri.ges.adapter.AdapterDefinitionBase;
import com.esri.ges.adapter.AdapterType;
import com.esri.ges.connector.Connector;
import com.esri.ges.connector.Connector.ConnectorType;
import com.esri.ges.connector.ConnectorProperty;
import com.esri.ges.connector.ConnectorProperty.Source;
import com.esri.ges.core.ConfigurationException;
import com.esri.ges.core.Uri;
import com.esri.ges.core.geoevent.DefaultFieldDefinition;
import com.esri.ges.core.geoevent.DefaultGeoEventDefinition;
import com.esri.ges.core.geoevent.FieldDefinition;
import com.esri.ges.core.geoevent.FieldType;
import com.esri.ges.core.geoevent.GeoEventDefinition;

public class TweetStatusAdapterDefinition extends AdapterDefinitionBase
{
  private String _defName = "TweetStatus";

  public TweetStatusAdapterDefinition(AdapterType type)
  {
    super(type);

    try
    {
      GeoEventDefinition md = new DefaultGeoEventDefinition();
      md.setName(_defName);
      List<FieldDefinition> fieldDefinitions = new ArrayList<FieldDefinition>();
      fieldDefinitions.add(new DefaultFieldDefinition("possibly_sensitive_editable", FieldType.Boolean));
      fieldDefinitions.add(new DefaultFieldDefinition("text", FieldType.String));
      fieldDefinitions.add(new DefaultFieldDefinition("created_at", FieldType.Date, "TIME_START"));
      fieldDefinitions.add(new DefaultFieldDefinition("retweeted", FieldType.Boolean));
      fieldDefinitions.add(new DefaultFieldDefinition("retweet_count", FieldType.Integer));
      fieldDefinitions.add(new DefaultFieldDefinition("coordinates", FieldType.Geometry, "GEOMETRY"));
      fieldDefinitions.add(new DefaultFieldDefinition("id_str", FieldType.String, "TRACK_ID"));
      fieldDefinitions.add(new DefaultFieldDefinition("in_reply_to_screen_name", FieldType.String));
      fieldDefinitions.add(new DefaultFieldDefinition("in_reply_to_status_id_str", FieldType.String));
      fieldDefinitions.add(new DefaultFieldDefinition("favorited", FieldType.Boolean));
      fieldDefinitions.add(new DefaultFieldDefinition("truncated", FieldType.Boolean));
      fieldDefinitions.add(new DefaultFieldDefinition("possibly_sensitive", FieldType.Boolean));
      fieldDefinitions.add(new DefaultFieldDefinition("in_reply_to_user_id_str", FieldType.String));

      FieldDefinition placeGroup = new DefaultFieldDefinition("place", FieldType.Group);
      placeGroup.addChild(new DefaultFieldDefinition("id", FieldType.String));
      placeGroup.addChild(new DefaultFieldDefinition("full_name", FieldType.String));
      placeGroup.addChild(new DefaultFieldDefinition("url", FieldType.String));
      fieldDefinitions.add(placeGroup);

      FieldDefinition userGroup = new DefaultFieldDefinition("user", FieldType.Group);
      userGroup.addChild(new DefaultFieldDefinition("id_str", FieldType.String));
      userGroup.addChild(new DefaultFieldDefinition("name", FieldType.String));
      userGroup.addChild(new DefaultFieldDefinition("followers_count", FieldType.Integer));
      userGroup.addChild(new DefaultFieldDefinition("location", FieldType.String));
      userGroup.addChild(new DefaultFieldDefinition("screen_name", FieldType.String));
      fieldDefinitions.add(userGroup);

      fieldDefinitions.add(new DefaultFieldDefinition("geolocated", FieldType.Boolean));

      md.setFieldDefinitions(fieldDefinitions);
      geoEventDefinitions.put(md.getName(), md);
    } catch (ConfigurationException ex)
    {
      ;
    }
  }

  @Override
  public String getName()
  {
    return "Twitter";
  }

  @Override
  public String getDomain()
  {
    return "com.esri.ges.adapter.inbound";
  }

  @Override
  public String getDescription()
  {
    return "This adapter is capable of receiving tweet status data from Twitter service.";
  }

  @Override
  public String getContactInfo()
  {
    return "ges@esri.com";
  }
  
  @Override
  public List<Connector> getConnectors()
  {
    ArrayList<Connector> connectors = new ArrayList<Connector>();

    // "Receive Tweets" Input Connector
    Connector receiveTweetsConnector =
        new Connector( "receive-tweets-connector",
                       "twitter-in",
                       ConnectorType.inbound,
                       new Uri("com.esri.ges.adapter.inbound" ,"Twitter", "10.2.0"),
                       new Uri("com.esri.ges.transport.inbound" ,"Twitter", "10.2.0"),
                       "Receive Tweets",
                       "Connects to the public Twitter API and receives Tweets based on profiles that you want to follow, terms you want to track, and within a location of interest."
                     );
    receiveTweetsConnector.addShownProperty(new ConnectorProperty(Source.transport, "consumerKey", "", "Consumer Key"));
    receiveTweetsConnector.addShownProperty(new ConnectorProperty(Source.transport, "consumerSecret", "", "Consumer Secret"));
    receiveTweetsConnector.addShownProperty(new ConnectorProperty(Source.transport, "accessToken", "", "Access Token"));
    receiveTweetsConnector.addShownProperty(new ConnectorProperty(Source.transport, "accessTokenSecret", "", "Access Token Secret"));
    receiveTweetsConnector.addShownProperty(new ConnectorProperty(Source.transport, "follow", "", "Follow"));
    receiveTweetsConnector.addShownProperty(new ConnectorProperty(Source.transport, "track", "", "Track"));
    receiveTweetsConnector.addShownProperty(new ConnectorProperty(Source.transport, "locations", "", "Locations"));
    receiveTweetsConnector.addAdvancedProperty(new ConnectorProperty(Source.transport, "count", "0", "Count"));
    connectors.add(receiveTweetsConnector);

    // "Send a Tweet" Output Connector
    Connector sendtweetConnector =
        new Connector( "send-tweet-connector",
                       "twitter-out",
                       ConnectorType.outbound,
                       new Uri("com.esri.ges.adapter.outbound" ,"MessageFormatter", "10.2.0"),
                       new Uri("com.esri.ges.transport.outbound" ,"Twitter", "10.2.0"),
                       "Send a Tweet",
                       "Connects to the public Twitter API and sends a formatted Tweet."
                     );
    sendtweetConnector.addShownProperty(new ConnectorProperty(Source.adapter, "textMessage", "Enter Message Body Here", "Message"));
    sendtweetConnector.addShownProperty(new ConnectorProperty(Source.transport, "consumerKey", "", "Consumer Key"));
    sendtweetConnector.addShownProperty(new ConnectorProperty(Source.transport, "consumerSecret", "", "Consumer Secret"));
    sendtweetConnector.addShownProperty(new ConnectorProperty(Source.transport, "accessToken", "", "Access Token"));
    sendtweetConnector.addShownProperty(new ConnectorProperty(Source.transport, "accessTokenSecret", "", "Access Token Secret"));
    connectors.add(sendtweetConnector);

    return connectors;
  }

  public String getGeoEventDefName()
  {
    return _defName;
  }
}
