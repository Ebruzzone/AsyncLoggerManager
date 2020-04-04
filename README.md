# AsyncLoggerManager

This logger has two important features:
 - Is asynchronous relatively to the main thread
 - The operations that are done by the main thread are equivalent to an if and an addition of an object to a list
 
 Thanks to these features, this logger is able to cut the time spent on logs.
 
 ###Performances
 
 In the main class there is a test of lots of logs.
 - The level of logs is Info so the Trace logs aren't logged. 
 - The number of info logs is 100000 repeated 30 times.
 - The number of trace logs is 2000000 repeated 30 times.
 - The length of each log is 256 characters.
 - The test was repeated three times.
 
| Level | AsyncLoggerManager | Apache AsyncLogger with disruptor |
| :---: | :----------------: | :-------------------------------: |
| Info  | 113 ms | 25884 ms |
| Info  | 135 ms | 27808 ms |
| Info  | 134 ms | 27947 ms |
| Trace | 128 ms | 425 ms |
| Trace | 137 ms | 344 ms |
| Trace | 140 ms | 378 ms |

###Reduced use of resources

With some tricks, the used resources are the least possible.
- A thread which is inactive most of the time
- Lists of strings where the logs are added and executed in order
- Nothing else
