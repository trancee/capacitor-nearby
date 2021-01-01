To generate the Java Lite protobuf files:

```sh
protoc --java_out=lite:android/src/main/java/$DST_DIR proto/*.proto
protoc --swift_out=ios/Plugin/ proto/*.proto
```
