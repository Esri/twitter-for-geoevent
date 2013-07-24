package com.esri.geoevent.transport.twitter;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.transport.Transport;
import com.esri.ges.transport.http.HttpInboundTransportService;
import com.esri.ges.transport.util.XmlTransportDefinition;

public class TwitterInboundTransportService extends HttpInboundTransportService
{
  public TwitterInboundTransportService()
  {
    super();
    definition = new XmlTransportDefinition(getResourceAsStream("twitter-inboundtransport-definition.xml"), super.definition);
  }

  public Transport createTransport() throws ComponentException
  {
    return new TwitterInboundTransport(definition);
  }

}
