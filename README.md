# twitter-for-geoevent

ArcGIS GeoEvent Processor Sample Twitter Inbound and Outbound Connectors for sending and receiving tweets.

![App](twitter-for-geoevent.png?raw=true)

## Features
* Twitter Inbound Adapter
* Twitter Inbound Transport
* Twitter Outbound Transport

## Instructions

Building the source code:

1. Make sure Maven and ArcGIS GeoEvent Processor SDK are installed on your machine.
2. Download and unzip the .zip file, or clone the repository.
3. Copy the ArcGIS GeoEvent Processor SDK 'repository' folder to be a sibling folder to this repo folder, or
4. Modify the main pom.xml 'ages-sdk-repo' repository section to point to the ArcGIS GeoEvent Processor SDK 'repository' folder.
5. Run 'mvn install -Dcontact.address=[YourContactEmailAddress]'

Installing the built jar files:

1. Copy the *.jar files under the 'target' sub-folder(s) into the [ArcGIS-GeoEvent-Processor-Install-Directory]/deploy folder.

## Requirements

* ArcGIS GeoEvent Processor for Server.
* ArcGIS GeoEvent Processor SDK.
* Java JDK 1.6 or greater.
* Maven.

## Resources

* [ArcGIS GeoEvent Processor for Server Resource Center](http://resources.arcgis.com/en/communities/geoevent)
* [ArcGIS Blog](http://blogs.esri.com/esri/arcgis/)
* [twitter@esri](http://twitter.com/esri)

## Issues

Find a bug or want to request a new feature?  Please let us know by submitting an issue.

## Contributing

Anyone and everyone is welcome to contribute. 

## Licensing
Copyright 2013 Esri

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

A copy of the license is available in the repository's [license.txt](license.txt?raw=true) file.

[](ArcGIS, GeoEvent, Processor)
[](Esri Tags: ArcGIS GeoEvent Processor for Server)
[](Esri Language: Java)