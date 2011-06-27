# Grails plugin for Pusher

This plugin is a wrapper for the [Pusher](http://pusher.com/) REST API.


### Installation
    grails install-plugin pusher

### Configuration

When installing the plugin, placeholders for your Pusher credentials are added to your Config.groovy file. 
Fill in the credentials:


    pusherapp.host = "api.pusherapp.com"
    pusherapp.applicationId = ""
    pusherapp.applicationKey = ""
    pusherapp.applicationSecret = ""
 

### PusherService

The plugin provides this service to communicate with the Pusher API.
Inject it in your artifacts declaring this attribute:

    def pusherService

Call one of the two methods called "triggerPush" and pass channel name, event name and the message body (JSON as String) as parameters:

    pusherService.triggerPush(channel, event, jsonData)

The second "triggerPush" method provides an additional parameter for the socket_id:

    pusherService.triggerPush(channel, event, jsonData, socketId);
