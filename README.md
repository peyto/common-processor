# Spring Processor Framework

Processor Framework is design to allow execution of a single-threaded long-running jobs with state <br>

## Processor
Processor should meet requirements:
* it can be executed any number of time, returning deterministic result for the same input conditions (state + timestamp + input receivers data)
* return BUSY/IDLE/END status
  * BUSY: processor done some job and should be executed again (e.g., it consumed and processed some input message, so it should be called again to check if there is any other input available)
  * IDLE: processor didn't do any useful job and can be put to sleep
  * END: processor should finish and shouldn't be called anymore
* init method for initialising processor
* end method for disposing resources on processor finish

