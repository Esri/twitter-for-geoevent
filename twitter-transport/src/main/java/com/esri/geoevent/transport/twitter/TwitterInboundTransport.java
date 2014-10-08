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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.TransportContext;
import com.esri.ges.transport.TransportDefinition;
import com.esri.ges.transport.http.HttpInboundTransport;
import com.esri.ges.transport.http.HttpTransportContext;

public class TwitterInboundTransport extends HttpInboundTransport
{
	static final private BundleLogger	LOGGER		= BundleLoggerFactory.getLogger(TwitterInboundTransport.class);

	private String										consumerKey;
	private String										consumerSecret;
	private String										accessToken;
	private String										accessTokenSecret;
	private String										postBodyOrg;

	private long[]										follows		= null;
	private String[]									tracks		= null;
	private double[][]								locations	= null;
	private int												count			= -1;

	public TwitterInboundTransport(TransportDefinition definition) throws ComponentException
	{
		super(definition);
	}

	@Override
	public synchronized void start()
	{
		super.start();
		LOGGER.debug("INBOUND_START");
	}

	@Override
	public synchronized void stop()
	{
		super.stop();
		LOGGER.debug("INBOUND_STOP");
	}

	@Override
	public synchronized void setup()
	{
		super.setup();
		try
		{
			applyProperties();
			// encode the postBody
			postBodyOrg = postBody;
			postBody = OAuth.encodePostBody(postBodyOrg);
			LOGGER.debug(postBody);
      consoleDebugPrintLn(postBody);
		}
		catch (Exception error)
		{
			LOGGER.error("INBOUND_TRANSPORT_SETUP_ERROR", error.getMessage());
			LOGGER.info(error.getMessage(), error);
		}
	}

	@Override
	public void beforeConnect(TransportContext context)
	{
		// String url = "https://stream.twitter.com/1/statuses/filter.json";

		HttpRequest request = ((HttpTransportContext) context).getHttpRequest();

		String authorizationHeader = OAuth.createOAuthAuthorizationHeader(clientUrl, httpMethod, postBodyOrg, accessToken, accessTokenSecret, consumerKey, consumerSecret);

		// logger.debug(authorizationHeader);
		request.addHeader(OAuth.AUTHORIZATION, authorizationHeader);
		request.addHeader(OAuth.ACCEPT, OAuth.ACCEPT_VALUES);// "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"
		request.setHeader(OAuth.CONTENT_TYPE, this.postBodyType);// "application/x-www-form-urlencoded"
	}

	@Override
	public void validate()
	{
		LOGGER.debug("INBOUND_SKIP_VALIDATION");
	}

	@Override
	public void onReceive(TransportContext context)
	{
		super.onReceive(context);
		consoleDebugPrintLn("INBOUND_ON_RECEIVE");
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

		StringBuilder paramsStr = new StringBuilder();
		if (getProperty("follow").isValid())
		{
			String value = (String) getProperty("follow").getValue();
			if (StringUtils.isNotEmpty(value))
			{
				paramsStr.append("follow=" + value);
				String[] flwStrs = value.split(",");
				if (flwStrs.length > 0)
				{
					follows = new long[flwStrs.length];
					for (int i = 0; i < flwStrs.length; i++)
					{
						follows[i] = Long.parseLong(flwStrs[i].trim());
					}
				}
			}
		}
		if (getProperty("track").isValid())
		{
			String value = (String) getProperty("track").getValue();
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
		if (getProperty("locations").isValid())
		{
			String value = (String) getProperty("locations").getValue();
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
					int dimension = length / 4;
					locations = new double[dimension][4];
					for (int i = 0; i < dimension; i++)
					{
						for (int j = 0; j < 4; j++)
						{
							locations[i][j] = Double.parseDouble(crdStrs[i + j].trim());
						}
					}
				}
			}
		}
		// required elevated access to use
		if (getProperty("count").isValid())
		{
			Object prop = getProperty("count").getValue();
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
		if (paramsStr.length() > 0)
		{
			postBody = paramsStr.toString();
		}
	}

	public static void consoleDebugPrintLn(String msg)
	{
		String consoleOut = System.getenv("GEP_CONSOLE_OUTPUT");
		if (consoleOut != null && "1".equals(consoleOut))
		{
			System.out.println(msg);
			LOGGER.debug(msg);
		}
	}

	public static void consoleDebugPrint(String msg)
	{
		String consoleOut = System.getenv("GEP_CONSOLE_OUTPUT");
		if (consoleOut != null && "1".equals(consoleOut))
		{
			System.out.print(msg);
			LOGGER.debug(msg);
		}
	}

}
