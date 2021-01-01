// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: proto/message.proto

package com.getcapacitor.plugin;

public final class Proto {
  private Proto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }
  public interface MessageOrBuilder extends
      // @@protoc_insertion_point(interface_extends:proto.Message)
      com.google.protobuf.MessageLiteOrBuilder {

    /**
     * <code>bytes uuid = 1;</code>
     * @return The uuid.
     */
    com.google.protobuf.ByteString getUuid();

    /**
     * <code>uint64 timestamp = 2;</code>
     * @return The timestamp.
     */
    long getTimestamp();

    /**
     * <code>bytes content = 3;</code>
     * @return The content.
     */
    com.google.protobuf.ByteString getContent();

    /**
     * <code>string type = 4;</code>
     * @return The type.
     */
    java.lang.String getType();
    /**
     * <code>string type = 4;</code>
     * @return The bytes for type.
     */
    com.google.protobuf.ByteString
        getTypeBytes();
  }
  /**
   * <pre>
   * [START messages]
   * </pre>
   *
   * Protobuf type {@code proto.Message}
   */
  public  static final class Message extends
      com.google.protobuf.GeneratedMessageLite<
          Message, Message.Builder> implements
      // @@protoc_insertion_point(message_implements:proto.Message)
      MessageOrBuilder {
    private Message() {
      uuid_ = com.google.protobuf.ByteString.EMPTY;
      content_ = com.google.protobuf.ByteString.EMPTY;
      type_ = "";
    }
    public static final int UUID_FIELD_NUMBER = 1;
    private com.google.protobuf.ByteString uuid_;
    /**
     * <code>bytes uuid = 1;</code>
     * @return The uuid.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString getUuid() {
      return uuid_;
    }
    /**
     * <code>bytes uuid = 1;</code>
     * @param value The uuid to set.
     */
    private void setUuid(com.google.protobuf.ByteString value) {
      value.getClass();
  
      uuid_ = value;
    }
    /**
     * <code>bytes uuid = 1;</code>
     */
    private void clearUuid() {
      
      uuid_ = getDefaultInstance().getUuid();
    }

    public static final int TIMESTAMP_FIELD_NUMBER = 2;
    private long timestamp_;
    /**
     * <code>uint64 timestamp = 2;</code>
     * @return The timestamp.
     */
    @java.lang.Override
    public long getTimestamp() {
      return timestamp_;
    }
    /**
     * <code>uint64 timestamp = 2;</code>
     * @param value The timestamp to set.
     */
    private void setTimestamp(long value) {
      
      timestamp_ = value;
    }
    /**
     * <code>uint64 timestamp = 2;</code>
     */
    private void clearTimestamp() {
      
      timestamp_ = 0L;
    }

    public static final int CONTENT_FIELD_NUMBER = 3;
    private com.google.protobuf.ByteString content_;
    /**
     * <code>bytes content = 3;</code>
     * @return The content.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString getContent() {
      return content_;
    }
    /**
     * <code>bytes content = 3;</code>
     * @param value The content to set.
     */
    private void setContent(com.google.protobuf.ByteString value) {
      value.getClass();
  
      content_ = value;
    }
    /**
     * <code>bytes content = 3;</code>
     */
    private void clearContent() {
      
      content_ = getDefaultInstance().getContent();
    }

    public static final int TYPE_FIELD_NUMBER = 4;
    private java.lang.String type_;
    /**
     * <code>string type = 4;</code>
     * @return The type.
     */
    @java.lang.Override
    public java.lang.String getType() {
      return type_;
    }
    /**
     * <code>string type = 4;</code>
     * @return The bytes for type.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getTypeBytes() {
      return com.google.protobuf.ByteString.copyFromUtf8(type_);
    }
    /**
     * <code>string type = 4;</code>
     * @param value The type to set.
     */
    private void setType(
        java.lang.String value) {
      value.getClass();
  
      type_ = value;
    }
    /**
     * <code>string type = 4;</code>
     */
    private void clearType() {
      
      type_ = getDefaultInstance().getType();
    }
    /**
     * <code>string type = 4;</code>
     * @param value The bytes for type to set.
     */
    private void setTypeBytes(
        com.google.protobuf.ByteString value) {
      checkByteStringIsUtf8(value);
      type_ = value.toStringUtf8();
      
    }

    public static com.getcapacitor.plugin.Proto.Message parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data);
    }
    public static com.getcapacitor.plugin.Proto.Message parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data, extensionRegistry);
    }
    public static com.getcapacitor.plugin.Proto.Message parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data);
    }
    public static com.getcapacitor.plugin.Proto.Message parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data, extensionRegistry);
    }
    public static com.getcapacitor.plugin.Proto.Message parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data);
    }
    public static com.getcapacitor.plugin.Proto.Message parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data, extensionRegistry);
    }
    public static com.getcapacitor.plugin.Proto.Message parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, input);
    }
    public static com.getcapacitor.plugin.Proto.Message parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, input, extensionRegistry);
    }
    public static com.getcapacitor.plugin.Proto.Message parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return parseDelimitedFrom(DEFAULT_INSTANCE, input);
    }
    public static com.getcapacitor.plugin.Proto.Message parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
    }
    public static com.getcapacitor.plugin.Proto.Message parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, input);
    }
    public static com.getcapacitor.plugin.Proto.Message parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, input, extensionRegistry);
    }

    public static Builder newBuilder() {
      return (Builder) DEFAULT_INSTANCE.createBuilder();
    }
    public static Builder newBuilder(com.getcapacitor.plugin.Proto.Message prototype) {
      return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
    }

    /**
     * <pre>
     * [START messages]
     * </pre>
     *
     * Protobuf type {@code proto.Message}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageLite.Builder<
          com.getcapacitor.plugin.Proto.Message, Builder> implements
        // @@protoc_insertion_point(builder_implements:proto.Message)
        com.getcapacitor.plugin.Proto.MessageOrBuilder {
      // Construct using com.getcapacitor.plugin.Proto.Message.newBuilder()
      private Builder() {
        super(DEFAULT_INSTANCE);
      }


      /**
       * <code>bytes uuid = 1;</code>
       * @return The uuid.
       */
      @java.lang.Override
      public com.google.protobuf.ByteString getUuid() {
        return instance.getUuid();
      }
      /**
       * <code>bytes uuid = 1;</code>
       * @param value The uuid to set.
       * @return This builder for chaining.
       */
      public Builder setUuid(com.google.protobuf.ByteString value) {
        copyOnWrite();
        instance.setUuid(value);
        return this;
      }
      /**
       * <code>bytes uuid = 1;</code>
       * @return This builder for chaining.
       */
      public Builder clearUuid() {
        copyOnWrite();
        instance.clearUuid();
        return this;
      }

      /**
       * <code>uint64 timestamp = 2;</code>
       * @return The timestamp.
       */
      @java.lang.Override
      public long getTimestamp() {
        return instance.getTimestamp();
      }
      /**
       * <code>uint64 timestamp = 2;</code>
       * @param value The timestamp to set.
       * @return This builder for chaining.
       */
      public Builder setTimestamp(long value) {
        copyOnWrite();
        instance.setTimestamp(value);
        return this;
      }
      /**
       * <code>uint64 timestamp = 2;</code>
       * @return This builder for chaining.
       */
      public Builder clearTimestamp() {
        copyOnWrite();
        instance.clearTimestamp();
        return this;
      }

      /**
       * <code>bytes content = 3;</code>
       * @return The content.
       */
      @java.lang.Override
      public com.google.protobuf.ByteString getContent() {
        return instance.getContent();
      }
      /**
       * <code>bytes content = 3;</code>
       * @param value The content to set.
       * @return This builder for chaining.
       */
      public Builder setContent(com.google.protobuf.ByteString value) {
        copyOnWrite();
        instance.setContent(value);
        return this;
      }
      /**
       * <code>bytes content = 3;</code>
       * @return This builder for chaining.
       */
      public Builder clearContent() {
        copyOnWrite();
        instance.clearContent();
        return this;
      }

      /**
       * <code>string type = 4;</code>
       * @return The type.
       */
      @java.lang.Override
      public java.lang.String getType() {
        return instance.getType();
      }
      /**
       * <code>string type = 4;</code>
       * @return The bytes for type.
       */
      @java.lang.Override
      public com.google.protobuf.ByteString
          getTypeBytes() {
        return instance.getTypeBytes();
      }
      /**
       * <code>string type = 4;</code>
       * @param value The type to set.
       * @return This builder for chaining.
       */
      public Builder setType(
          java.lang.String value) {
        copyOnWrite();
        instance.setType(value);
        return this;
      }
      /**
       * <code>string type = 4;</code>
       * @return This builder for chaining.
       */
      public Builder clearType() {
        copyOnWrite();
        instance.clearType();
        return this;
      }
      /**
       * <code>string type = 4;</code>
       * @param value The bytes for type to set.
       * @return This builder for chaining.
       */
      public Builder setTypeBytes(
          com.google.protobuf.ByteString value) {
        copyOnWrite();
        instance.setTypeBytes(value);
        return this;
      }

      // @@protoc_insertion_point(builder_scope:proto.Message)
    }
    @java.lang.Override
    @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
    protected final java.lang.Object dynamicMethod(
        com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
        java.lang.Object arg0, java.lang.Object arg1) {
      switch (method) {
        case NEW_MUTABLE_INSTANCE: {
          return new com.getcapacitor.plugin.Proto.Message();
        }
        case NEW_BUILDER: {
          return new Builder();
        }
        case BUILD_MESSAGE_INFO: {
            java.lang.Object[] objects = new java.lang.Object[] {
              "uuid_",
              "timestamp_",
              "content_",
              "type_",
            };
            java.lang.String info =
                "\u0000\u0004\u0000\u0000\u0001\u0004\u0004\u0000\u0000\u0000\u0001\n\u0002\u0003" +
                "\u0003\n\u0004\u0208";
            return newMessageInfo(DEFAULT_INSTANCE, info, objects);
        }
        // fall through
        case GET_DEFAULT_INSTANCE: {
          return DEFAULT_INSTANCE;
        }
        case GET_PARSER: {
          com.google.protobuf.Parser<com.getcapacitor.plugin.Proto.Message> parser = PARSER;
          if (parser == null) {
            synchronized (com.getcapacitor.plugin.Proto.Message.class) {
              parser = PARSER;
              if (parser == null) {
                parser =
                    new DefaultInstanceBasedParser<com.getcapacitor.plugin.Proto.Message>(
                        DEFAULT_INSTANCE);
                PARSER = parser;
              }
            }
          }
          return parser;
      }
      case GET_MEMOIZED_IS_INITIALIZED: {
        return (byte) 1;
      }
      case SET_MEMOIZED_IS_INITIALIZED: {
        return null;
      }
      }
      throw new UnsupportedOperationException();
    }


    // @@protoc_insertion_point(class_scope:proto.Message)
    private static final com.getcapacitor.plugin.Proto.Message DEFAULT_INSTANCE;
    static {
      Message defaultInstance = new Message();
      // New instances are implicitly immutable so no need to make
      // immutable.
      DEFAULT_INSTANCE = defaultInstance;
      com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
        Message.class, defaultInstance);
    }

    public static com.getcapacitor.plugin.Proto.Message getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static volatile com.google.protobuf.Parser<Message> PARSER;

    public static com.google.protobuf.Parser<Message> parser() {
      return DEFAULT_INSTANCE.getParserForType();
    }
  }


  static {
  }

  // @@protoc_insertion_point(outer_class_scope)
}
