# Core Concepts

Today's systems collect more logging, handle more transactions, report more
analytics, and define more complex relationships than ever before. This growth in
data sizes strains the classic model of object mapping to its breaking point.

Convirgance embraces modern data by building upon byte-by-byte streams (also
known as unix streams) to implement a Data Flow pattern. Bytes are translated
into records which can be easily transformed and then serialized back into a
unix stream of bytes once processed.

## Records

The core unit of data in Convirgance is a record. Records are represented by
`JSONObject` which is an implementation of the Java Collections `Map` interface.
The implementation is designed to easily parse and print to JSON making debugging
incredibly easy and complex test cases a breeze.

Data is stored in `JSONObject` as key/value pairs. This aligns with both the
JSON specification and the requirements of the `Map` interface.

Implementing the `Map` interface additionally makes each record compatible with
any Java APIs or features that support `Map`. For example, JSP pages can utilize
native JSTL syntax on a `JSONObject` when rendering.

## Streams

Java enhances the idea of unix streams (represented by `InputStream` and
`OutputStream`) with the concept of `Iterator`. An iterator is a stream of data
types a level above a byte stream. Each entry is a Java `Object`, the type of
which is specified using a generic.

Convirgance hooks into this feature of the language by implementing its streams
of records as `Iterator<JSONObject>`.

The only issue with Iterators in Java is that they don't have full language
support. The `Iterator` object is already in process of streaming the data by
the time it exists. Thus Java introduced the concept of `Iterable` to provide
a mechanism for delivering streams on demand. Convirgance supports this approach
by using `Iterable<JSONObject>` as its primary interface for setting up streams.

Thus it becomes easy to use features like the enhanced `for` loop:

```java
FileSource file = new FileSource("data.json");
Iterable<JSONObject> stream = new JSONInput().read(file);

for(JSONObject record : stream)
{
    // Pretty prints each record as JSON
    System.out.println(record.toString(4));
}
```

A critical point about iterating over streams is that they do not
load the full data set into memory. The data is transformed into records as it is read
from the underlying byte stream. As long as each record can
fit into memory <u>Convirgance can handle an unlimited number of records</u>.

## Transformations

The basic pattern of transformations in Convirgance is to pass an `Iterable` into
a transformer and get a new `Iterable` back out. For example:

```java
FileSource file = new FileSource("data.csv");
Iterable<JSONObject> stream = new CSVInput().read(file);

// Transform the stream by parsing string values into numbers and booleans
stream = new CoerceStringsTransformer().transform(stream);
```

Each transformation places a processing step on records as they pass through the
stream. Critically, there is only one record in the stream at a time. That record
steps through each transformation and then is released once all operations have
been completed on it.

This approach is important because it minimizes memory usage, aligns data with
the Young GC, and maximizes the use of the CPUs L1 and L2 caches. For
data requiring a large number of operations, performance can improve by orders
of magnitude.

Note that the `Iterable` in/out approach implies that transformers can manipulate
the data in any manner they choose. Transformers can directly update each record
resulting in one record in and one record out. Additionally transformers can group and aggregate
data, expand the number of records, or reduce the number of records by filtering data out of the stream.

<!-- TODO Wording could be better here, it feels like I'm tripping over 'expand the number of records'  -->

## Filters

Filters are an extension to transformers that specifically reject records
based upon a "predicate", a condition that must be met for the
record to be kept.

Convirgance supports many of the types of [filters](filtering-data.md) you would
expect in a SQL engine. Including equals, greater than, less than, etc. Filters
based on boolean logic can be combined to create and/or/not logic.

`Filter` also implements the `java.util.function.Predicate` interface to be compatible with
Java functional programming techniques. Implementing a filter is accomplished
in a very similar manner and can be done with either a class, anonymous inner
class, or lambda arrow function. For example:

```java
// Find Bob
Filter bob = new Filter() {

    @Override
    public boolean test(JSONObject record)
    {
        String name = record.getString("name");

        return (name != null && name.contains("Bob"));
    }
};

// Find Rob
Filter rob = (JSONObject record) -> {
    return !record.isNull("name") && record.getString("name").contains("Rob");
};

// Find Bob or Rob
Filter or = new OrFilter(bob, rob);

// Find everyone except Bob and Rob
Filter not = new NotFilter(or);
```

## Source and Target

While `InputStream` and `OutputStream` are fantastic standard representations of
unix streams, they have the same problem as the `Iterator` in that the streams
are active as soon as they exist.

This means these standard library classes play poorly with the use of
`Iterable` that allows the pipeline
of transformations to be pre-planned and `Iterable` instances to be reused
throughout the code. To provide this compatibility, Convirgance introduces
`Source` and `Target` classes analogous to `InputStream` and `OutputStream`
respectively.

`Source` objects represent a plan to access the `InputStream` for a file, url,
byte buffer, and numerous other sources. Implementations attempt to reopen a
stream fresh whenever possible. When not possible, a clear error is thrown that
the stream is unavailable. APIs on `Source` can be interrogated to know if it
is reusable and if it has already been used.

`Target` is the companion for `OutputStream`. It represents a plan to write data
back to a resource. While it has the same support for reuse and detecting if
the stream has already been reused, engineers need to be aware that reuse may
result in data being overwritten. e.g. Writing to a `FileTarget` multiple
times will result in the file being overwritten.

## Input and Output

The process of converting the underlying stream of bytes into a stream of records
is handled by implementations of the `Input` interface. The `Input` implementation
reads from a provided `Source` to return an `Iterable<JSONObject>` stream.

`Output` provides the inverse concept, converting an `Iterable<JSONObject>` stream
into a serialized byte stream that is written to the provided `Target`.

Convirgance ships with support for numerous formats such as JSON, CSV,
tab-delimited, pipe-delimited, and even binary encodings. Additional formats
can be plugged in using the `Input` and `Output` concepts, thereby making
Convirgance a universal data processing platform.

## Sections

##### [Previous: Getting Started](./getting-started?id=getting-started-with-convirgance)

##### [Next: Database Operations](./database-operations?id=database-operations)
