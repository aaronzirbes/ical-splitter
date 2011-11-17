/**
	ICalDiff.groovy

	No dependencies other than Groovy itself.

	{@link https://github.com/aaronzirbes/ical-splitter}

	@author	Aaron J. Zirbes	(ajz@umn.edu)
	@version 1.0.0

*/

class ICalDiff {

	/** The main method, this parses out the filename, and calls splitFile().

		@param args		The arguments, in order, of source calendar, and the calendar missing events to compare to.
	 */
	static void main(String[] args) {

		if (args.size() == 2) {

			def sourceFileName = args[0]
			def compareToFileName = args[1]

			def sourceFile = new File(sourceFileName)
			def compareToFile = new File(compareToFileName)

			if (! sourceFile.exists()) {
				println "File not found: ${sourceFile}"
				displayHelp()
			} else if (! compareToFile.exists()) {
				println "File not found: ${compareToFile}"
				displayHelp()
			} else {
				println "Comparing files: '${sourceFile}' and '${compareToFile}'"

				diffFiles(sourceFile, compareToFile)
			}
		} else {
			displayHelp()
		}
	}

	/** This just prints out the help message if you don't pass a file name. */
	static void displayHelp () {
		println """
		Usage: groovy ICalDiff.groovy [fullcalendar.ics] [calendarmissingevents.ics]

		This program compares one iCal file to another iCal file, and creates a new
		iCal file containing all the events missing from the second iCal file.
		"""
	}

	/** This returns a File object pointing to a particular file split chunk.
	 
	 	@param inputFile	The file being split
		@param splitNumber	The number of the file split chunk
		@return	the file object representing the file split chunk
	 */
	static File getDiffFile(File inputFile) {
		new File(inputFile.name + '.diff')
	}

	/**
		This is the main driver for the script.

		@param	inputFile	the file being processed
		@param	eventsPerSplit	the maximum number of events to put in each file split chunk
	*/
	static void diffFiles(File sourceFile, File compareToFile) {

		def header = readHeader(sourceFile)
		def footer = 'END:VCALENDAR'
		def diffFile = getDiffFile(sourceFile)
		def sourceEvents = loadEvents(sourceFile)
		def compareEvents = loadEvents(compareToFile)
		def missingEvents = [] as Set

		// find out what's missing
		sourceEvents.each{ event ->
			if ( ! compareEvents.find{ it == event } ) {
				missingEvents << event
			}
		}

		println "'${compareToFile}' is missing ${missingEvents.size()} events from ${sourceFile}."

		// Write the missing file
		diffFile.write(header)
		missingEvents.each { event ->
			// println "Missing: ${event.uid}"
			diffFile << event
		}
		diffFile << footer
	}

	/**
		This reads the iCal header from an ical file and
		returns it as a string.

		@param	inputFile	the file being processed
		@return	the iCal header as a string
	*/
	static String readHeader(File inputFile) {
		def header = new StringBuffer()
		def finishedReadingHeader = false

		inputFile.eachLine{ line ->
			if ( ! finishedReadingHeader ) {
				// increment our event counter if needed
				if ( line == 'BEGIN:VEVENT' ) {
					// This marks the end of the header
					finishedReadingHeader = true
				} else {
					// append to the header if we're still in it
					header.append(line + '\n')
				}
			}
		}
		header.toString()
	}

	/**
		This loads ical event from a file into memory.

		@param	inputFile	the file being processed
		@return	a Set of ICalEvent instances representing all of the events in the file
	*/
	static loadEvents(File inputFile) {
		def eventCount = 0
		def iCalEventInstance = null
		def iCalEventInstanceList = [] as Set

		inputFile.eachLine{ line ->
			if ( line == 'BEGIN:VEVENT' ) {
				// increment our event counter
				eventCount++
				// create a new event
				iCalEventInstance = new ICalEvent()
			} else if ( line == 'END:VEVENT' ) {
				// add the event to the list
				if (iCalEventInstance.uid) {
					iCalEventInstanceList << iCalEventInstance
				} else {
					println "!!! FAILED to read UID from event:"
					println iCalEventInstance
				}
				iCalEventInstance = null
			} else if (iCalEventInstance) {
				// parse a line to set the event parameters
				iCalEventInstance.parseLine(line)
			}
		}

		if (iCalEventInstance) {
			iCalEventInstanceList.add(iCalEventInstance)
		}

		println "Processed ${eventCount} of ${iCalEventInstanceList.size()} records from '${inputFile}'."

		return iCalEventInstanceList 
	}
}

/**
	This represents a single iCal event.
	It is used to store the iCal events when loading them into memory.
*/
class ICalEvent {
	/** The UID setting for the Event */
	String uid
	/** The DTSTART setting for the event */
	Date dtStart
	/** The CREATED setting for the Event */
	String created
	/** The DTSTAMP setting for the Event */
	String dtStamp
	/** The full contents of the record as a String */
	String record

	/** parses a line from an iCal event, and places the data
	in the appropriate attribute */
	void parseLine(String setting) {
		def key = setting.replaceAll(/:.*/, '')
		def value = setting.replaceFirst(key + ':', '').trim()
		if (key == 'UID') { uid = value }
		else if (key == 'DTSTART') { dtStart = parseDate(value) }
		else if (key == 'CREATED') { created = value }
		else if (key == 'DTSTAMP') { dtStamp = parseDate(value) }
		
		record += setting + '\n'
	}

	/** this is used to parse iCal formated dates */
	private Date parseDate(String value) {
		def longDate = ~/[0-9]*T[0-9]*Z/
		def mediumDate = ~/[0-9]*T[0-9]*/
		def shortDate = ~/[0-9]*/

		if ( longDate.matcher(value).matches() ) {
			Date.parse("yyyyMMdd'T'HHmmss'Z'", value)
		} else if ( mediumDate.matcher(value).matches() ) {
			Date.parse("yyyyMMdd'T'HHmmss", value)
		} else if ( shortDate.matcher(value).matches() ) {
			Date.parse("yyyyMMdd", value)
		} else {
			null
		}
	}

	/** The default comparator for this class */
	int compareTo(iCalEvent) {
		uid.compareTo(iCalEvent.uid)
	}

	/** The default comparator for this class */
	boolean equals(iCalEvent) {
		if (iCalEvent instanceof ICalEvent) {
			return uid.equals(iCalEvent.uid)
		} else { return false }
	}

	/** The default toString() method for this class */
	String toString() {
		'BEGIN:VEVENT\n' + record + 'END:VEVENT\n'
	}
}
