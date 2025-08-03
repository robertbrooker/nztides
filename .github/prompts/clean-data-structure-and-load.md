# Clean Data Structure and Load
- This will focus on the main tide data loading for the display on screen. 
- We will deal with the notifications later

## Goal 
- Use initial load of data with loadPortData returning a list of TideRecord
- Load all the data we need into a data structure called 'TideData' so that different parts of the code don't need to go back to the TideService
- Verify the data as we load it, so that we don't need verification code everywhere
- We return trusted data only

## TideService
- keep loadPortData mostly unchanged except the below data verification changes

## CurrentTidesService
- This layer calls loadPortData and enriches a full data structure using a time stamp that is passed in from the top
- do all the validation relating to current time stamp

## TideData
- contain the list of TideRecords, a TideCalculation, current time stamp, Port, the last tiderecord, a TideInterval (or just move those properties into TideData) and any other data needed for the screen display
## Data verification
- verify the data once during load. If it is not valid throw an exception that later gets displayed
- At the bottom level loadPortData should check for at least 2 records, if there aren't it should throw (it doesn't know about the current time so that is all it can check)
```
 if (numRecords < 2) {
	// throw an exception
 }
```
- don't return any nulls, throw instead
- don't check for nulls everywhere
- in CurrentTidesService we should check we have current data and enough records for 30 days
- we don't use methods like isValidAt everywhere (only check on load)

## calculateTideOutputFromData
I don't like calculateTideOutputFromData it should not use tideService. All the the required data should be loaded into one data structure and passed in. Also, verification like this should be done in the CurrentTidesService
```
            // Check if we have valid data for current time

            if (!tideService.isValidAt(tides, currentTimeSeconds)) {

                outputString.append("Tide data has expired. Please update the app for current predictions.");

                return outputString.toString();

            }

            TideInterval interval = tideService.getTideInterval(tides, currentTimeSeconds);

            if (interval == null || !interval.isValid()) {

                outputString.append("No tide data available for the current time at ").append(port);

                return outputString.toString();

            }
```

## models folder
- move all the tide models into a models folder
  
---
make this a property on TideInterval call 'incomingTide':
tideData.currentInterval.previous.height < tideData.currentInterval.next.height

simplify displayTideTimings
- pass in TideData
- move  
```        long timeToPrevious = (currentTimeSeconds - previousTide.timestamp);
        long timeToNext = (nextTide.timestamp - currentTimeSeconds);
```
 to CurrentTidesService and use this to set properties on TideInterval and add 'closestTide' which has a  and an isClosestTideInTheFuture property

Don't reload the data here:
```
            CurrentTidesService currentTidesService = new CurrentTidesService(this);
            TideData tideData = currentTidesService.loadTideData(port, System.currentTimeMillis() / 1000);
            return calculateTideOutputFromData(tideData);
```
instead save the list of TideRecords on the class and RefreshData method of the CurrentTidesService, pass in the list and the new current time stamp to regenerate TideData from the static data in the list
