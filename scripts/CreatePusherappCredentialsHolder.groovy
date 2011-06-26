includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsSettings")

target(createPusherappCredentialsHolder: "Updates Config.groovy with default holders for Pusherapp credentials") {
	def configFile = new File(basedir, "grails-app/conf/Config.groovy")
	if (configFile.exists()) {
		def appConfig = configSlurper.parse(configFile.toURI().toURL())
		if (appConfig.pusherapp.host) {
			event "StatusUpdate", ["Config.groovy already contains credentials holder for Pusherapp"]
		} else {
			event "StatusUpdate", ["Adding creadentials holder to Config.groovy"]
			configFile.withWriterAppend {
				it.write """

// Added by the Pusher plugin:
pusherapp.host = "api.pusherapp.com"
pusherapp.applicationId = ""
pusherapp.applicationKey = ""
pusherapp.applicationSecret = ""
"""
			}
		}
	}
}

setDefaultTarget(createPusherappCredentialsHolder)