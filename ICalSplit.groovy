/**
	ICalSplit.groovy

	No dependencies other than Groovy itself.

	{@link https://github.com/aaronzirbes/ical-splitter}

	@author	Aaron J. Zirbes	(ajz@umn.edu)
	@version 1.0.0

*/

class ICalSplit {

	/** This static setting determines the maximum number of events per file split */
	static def maxEventsPerFile = 1000

	/** The main method, this parses out the filename, and calls splitFile().

		@param args		The single argument of the file name to split
	 */
	static void main(String[] args) {

		if (args) {
			def fileName = args[0]
			def inputFile = new File(fileName)

			if (inputFile.exists()) {
				println "Splitting file: ${fileName}"

				splitFile(inputFile, maxEventsPerFile)

			} else {
				println "File not found: ${fileName}"
				displayHelp()
			}
		} else {
			displayHelp()
		}
	}

	/** This just prints out the help message if you don't pass a file name. */
	static void displayHelp () {
		println """
		Usage: groovy ICalSplit.groovy [yourcalendar.ics]

		This program splits an iCal file in to chunks of ${maxEventsPerFile} events each
		"""
	}

	/** This returns a File object pointing to a particular file split chunk.
	 
	 	@param inputFile	The file being split
		@param splitNumber	The number of the file split chunk
		@return	the file object representing the file split chunk
	 */
	static File getSplitFile(File inputFile, splitNumber) {
		new File(inputFile.name + '.' + splitNumber.toString().padLeft(3,'0'))
	}

	/**
		This is the main driver for the script.

		@param	inputFile	the file being processed
		@param	eventsPerSplit	the maximum number of events to put in each file split chunk
	*/
	static void splitFile(File inputFile, Integer eventsPerSplit) {
		def header = new StringBuffer()
		def footer = 'END:VCALENDAR'
		def eventCount = 0
		def eventSplit = 0
		def splitNumber = 1
		def splitFileChunk = getSplitFile(inputFile, splitNumber)
		println "Writing ${splitFileChunk}"

		inputFile.eachLine{ line ->
			// increment our event counter if needed
			if ( line == 'BEGIN:VEVENT' ) {
				// append the header to the split file
				if (eventCount == 0) {
					splitFileChunk << header.toString()
				}
				eventCount++
				eventSplit++
			}

			// see if we've hit our quota for this chunk yet.
			if (eventSplit > eventsPerSplit) {
				// reset the split counter
				eventSplit = 1
				// increment the split number
				splitNumber++
				// write the footer to the split file
				splitFileChunk << footer
				// get a new split file
				splitFileChunk = getSplitFile(inputFile, splitNumber)
				println "Writing ${splitFileChunk}"
				// write the header to the split file
				splitFileChunk << header.toString()
			}

			// check to see if we've gotten to any events yet.
			if ( eventCount == 0 ) {
				// append to the header if we're still in it
				header.append(line + '\n')
			} else {
				// append to the split file
				splitFileChunk << line + '\n'
			}
		}

		// write the footer to the split file
		splitFileChunk << footer

	}
}
