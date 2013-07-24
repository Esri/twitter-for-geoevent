package com.esri.geoevent.transport.twitter;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.transport.Transport;
import com.esri.ges.transport.http.HttpOutboundTransportService;
import com.esri.ges.transport.util.XmlTransportDefinition;

public class TwitterOutboundTransportService extends HttpOutboundTransportService
{
  public TwitterOutboundTransportService()
  {
    super();
    definition = new XmlTransportDefinition(getResourceAsStream("twitter-outboundtransport-definition.xml"), super.definition);
  }

  @Override
  public Transport createTransport() throws ComponentException
  {
    return new TwitterOutboundTransport(definition);
  }

}
