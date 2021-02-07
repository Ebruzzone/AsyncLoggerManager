# AsyncLoggerManager

This logger has two important features:
 - Is asynchronous relatively to the main thread
 - The operations that are done by the main thread are equivalent to an if and an addition of an object to a list
 
 Thanks to these features, this logger is able to cut the time spent on logs.
 
 ### Performances
 
 In the main class there is a test of lots of logs.
 - The level of logs is Info so the Trace logs aren't logged. 
 - The number of info logs is 100K repeated 30 times.
 - The number of trace logs is 10M repeated 30 times.
 - The length of each log is 256 characters.
 - The test was repeated three times.
 
| Level | AsyncLoggerManager | Apache AsyncLogger with disruptor | No Operation |
| :---: | :----------------: | :-------------------------------: | :----------: |
| Info  | 90 ms | 15809 ms | 2 ms |
| Info  | 94 ms | 15891 ms | 2 ms |
| Info  | 84 ms | 15543 ms | 2 ms |
| Trace | 346 ms | 614 ms | 202 ms |
| Trace | 337 ms | 621 ms | 197 ms |
| Trace | 341 ms | 598 ms | 192 ms |

### Reduced use of resources

With some tricks, the used resources are the least possible.
- A thread which is inactive most of the time
- Lists of strings where the logs are added and executed in order
- Nothing else
