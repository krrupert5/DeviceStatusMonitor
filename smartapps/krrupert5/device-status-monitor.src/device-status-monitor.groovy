/**
 *  Device Status Monitor
 *
 *  Copyright 2016 Kyle Rupert
 *
 *  Version 1.0.2	1 Sep 2016
 * 
 *  Version History
 * 
 *  1.0.0 	30 Aug 2016		Initial Release
 *	1.0.1	31 Aug 2016		Fixed an issue with the threshold calculation
 *  1.0.2 	1  Sep 2016		Added battery capability, changed the duration to days, split the preferences into multiple pages
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
 
definition(
    name: "Device Status Monitor",
    namespace: "krrupert5",
    author: "Kyle Rupert",
    description: "This app checks to make sure that devices have reported a status change (temperature, battery, etc.) within a certain time period.  This helps to catch devices that have been disconnected from the network and need to be reset.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "selectDevices", title: "Select Devices to Monitor", nextPage: "selectPollSettings", uninstall: true) {
        section("Temperatures to monitor") {
            input "temperatures", "capability.temperatureMeasurement", required: false, title: "Temperatures: ", multiple: true
        }
        
        section("Warning") {
        	paragraph "Battery statuses are not updated by most devices as often as temperatures, if using battery to monitor a device then make sure the threshold is larger (~ 7 days)"
        }
        
        section("Battery Status to monitor") {
        	input "batteries", "capability.battery", required: false, title: "Batteries: ", multiple: true
        }

        section("Contacts to monitor") {
            input "contacts", "capability.contactSensor", required: false, title: "Contacts: ", multiple: true
        }   
    }
    
    page(name: "selectPollSettings", title: "Select the Polling Settings", nextPage: "setNotifications") {
 	    section("Check Every") {
            input "days", "enum", required: true, title: "Days: ", options: [1, 2, 3, 4, 5, 6, 7]
        }   
    }
    
    page(name: "setNotifications", title: "Set the Notification Settings", install: true) {
        section("Notifications") {
            input("recipients", "contact", title: "Send notifications to", required: false) {            
                input "sendPushMessage", "enum", title: "Send a Push Notification?", options: ["Yes", "No"], required: false
                input "phone", "phone", title: "Send a Text Message?", required: false
            }            
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {   
    schedule("0 0 0 1/$days * ? *", handlerMethod)
}

def handlerMethod() {  
    def daysInt = 1

    switch(days) {
    	case "1":
        	daysInt = 1
            break
        case "2":
        	daysInt = 2
            break
        case "3":
        	daysInt = 3
        	break
        case "4":
        	daysInt = 4
            break
        case "5":
        	daysInt = 5
            break
        case "6":
        	daysInt = 6
            break
        case "7":
        	daysInt = 7
            break
    }
        
    temperatures.each { temp ->
    	checkDevice(temp, "temperature", daysInt)
    }
    
    batteries.each { battery ->
    	checkDevice(battery, "battery", daysInt)
    }
    
    contacts.each { contact ->
    	checkDevice(contact, "contact", daysInt)
    }
}

private checkDevice(device, checkState, daysInt)
{
    def state  = device.currentState(checkState)
    def time = state.date.time
    def elapsedTime = now() - time
    def thresholdTime = daysInt * 24 * 60 * 60 * 1000 // ms = days 24 (hours/day) * 60 (min/hours) * 60 (sec/min) * 1000 (ms/sec) 

    log.debug "$checkState Device Status Check - Device: $device , Elapsed Time: $elapsedTime , Threshold: $thresholdTime"

    if (elapsedTime >= thresholdTime) {
        // Device has stopped responding
        def message = "$device ($checkState) Stopped Responding, Reset Device"
        send(message)
    }
}

private send(msg) {
	if ( sendPushMessage == "Yes" ) {
    	log.debug("sending push message")
        sendPush (msg)
    }
    
    if ( phone ) {
    	log.debug("sending text message to $phone")
        sendSms(phone, msg)
    }
    
    log.debug msg
}
