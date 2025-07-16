# Custom instructions for Copilot

## Project context
This Android app displays New Zealand tide data to users using ascii art to display an cosinusoidally diagram with a line representing the current time. 
This helps represent the current height and time from the last or the time to next tide. Also displays lines of tide data representing about 40 days worth.

### Data Source
- The tide data is sourced from the LINZ (Land Information New Zealand) website.
- The data is available in CSV format and includes information about tide heights and times.
- About 2 or 3 years worth of static data is downloaded from the Linz website using `tidelookup\downloadcsvfiles.py`.
- There is one file per port per year, e.g. `tidelookup\csvfiles\Auckland_2027.csv`.
- The CSV data is processed into {Port Name}.tdat files and stored in the app's assets folder for offline access using `tidelookup\binaryise_csv_tidetables.py`. e.g. `nztides_app\app\src\main\assets\Ben Gunn Wharf.tdat`

### Architecture & Communication
- Python for downloading static data
- Java for Android app development
    - The Android app lives in the `nztides_app` folder.


## Coding style
- Follow the **KISS principle** - prefer simple, readable code over complex solutions.
- Follow a **functional programming style** in C# wherever possible.
- Use **expression-bodied members** where applicable.
- Favor **pure functions** and avoid side effects.
- Prefer **higher-order functions** and **LINQ** over loops.
- Use **records** for modeling data instead of mutable classes.
- Prefer **pattern matching** over traditional `if` or `switch` statements.
- Avoid static state or global variables.

