# AMQP messages

## `whirled.money.GetBarCountMessage@whirled`
Message to retrieve the number of bars for a particular user.
```cpp
struct
{
    int32_t accountNameLength;
    char[accountNameLength] accountName;
};
```

Returns:
```cpp
struct
{
    int32_t numBars;
}
```

## `whirled.money.BarsBoughtMessage@whirled`
Message indicating a user purchased some number of bars.
```cpp
struct
{
    int32_t accountNameLength;
    char[accountNameLength] accountName;
    int32_t numBars;
    int32_t paymentLength;
    char[paymentLength] paymentLength; // something like "$2.95", I'm hoping
};
```

## `whirled.money.SubscriptionEndedMessage@whirled`
Message indicating a subscription payment was processed.
```cpp
struct
{
    int32_t accountNameLength;
    char[accountNameLength] accountName;
};
```

## `whirled.money.SubscriptionBilledMessage@whirled`
Message indicating a subscription payment was processed.
```cpp
struct
{
    int32_t accountNameLength;
    char[accountNameLength] accountName;
    int32_t months;
};
```