To generate the Java protobuf files:

```sh
protoc --java_out=android/src/main/java/$DST_DIR proto/*.proto
protoc --swift_out=ios/Plugin/ proto/*.proto
```
