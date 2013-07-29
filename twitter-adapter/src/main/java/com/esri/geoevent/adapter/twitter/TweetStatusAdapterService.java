package com.esri.geoevent.adapter.twitter;

import javax.xml.bind.JAXBException;

import com.esri.ges.adapter.Adapter;
import com.esri.ges.adapter.AdapterServiceBase;
import com.esri.ges.adapter.util.XmlAdapterDefinition;
import com.esri.ges.core.component.ComponentException;

public class TweetStatusAdapterService extends AdapterServiceBase
{
  public TweetStatusAdapterService()
  {
    XmlAdapterDefinition xmlAdapterDefinition = new XmlAdapterDefinition(getResourceAsStream("tweetstatus-adapter-definition.xml"));
    try
    {
      xmlAdapterDefinition.loadConnector(getResourceAsStream("input-connector-definition.xml"));
    }
    catch (JAXBException e)
    {
      throw new RuntimeException(e);
    }
    definition = xmlAdapterDefinition;
  }

  @Override
  public Adapter createAdapter() throws ComponentException
  {
    return new TweetStatusAdapter(definition);
  }

}
