# hashmap-experiments

A collection of `java.util.Map` implementations to demonstrate the techniques, trade-offs & performance of different 
strategies.



### Implementations

 - `FixedSizeMapWithLinkedListChaining` - fixed size map with separate chaining implemented via linked lists.
 - `FullCopyResizeMapWithLinkedListChaining` - map which resizes automatically when `loadFactor` >= `0.75` by performing a full table copy.
  
## Resources

 - Hash Table: https://en.wikipedia.org/wiki/Hash_table