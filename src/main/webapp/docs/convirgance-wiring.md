# Wiring Compositional Configuration

The Convirgance approach eliminates hard-coding and makes software more
configuration-driven. This creates a need for a common configuration format that 
can compose functionality.

Convirgance (Wiring) provides that facility with an easy to use and understand
XML format for instantiating complex object trees. Built 
originally for OLAP and Web Services, the solution is widely applicable to any
complex system where a configuration format is needed.

## Installation

> ![WARNING](images/warning.svg) **<font color="#AA9900">WARNING:</font>**
> Convirgance (Wiring) is in pre-release and may be subject to changes in the APIs

Add the following dependency to your Maven `pom.xml` file:

```xml
<dependency>
    <groupId>com.invirgance</groupId>
    <artifactId>convirgance-wiring</artifactId>
    <version>0.2.0</version>
</dependency>
```

# Structure

The structure of a Wiring file following this basic structure:

```xml
<object>
    <property>
        <value></value>
    </property>
</object>
```

`object` is the Java Object being constructed, `property` is the setter/getter
pair to set the value on, and `value` is the data type to set. 

Explicit definition of the value type is optional. If no tag is provided, the 
value will be interpreted as a string. `CDATA` sections can be used for complex
strings with special characters such as embedded SQL.

For example, the following XML configures an object called `TestBean`:

```xml
<object class="com.test.TestBean">
    <message>Hello world!</message>
</object>
```

The object might look like this:

```java
@Wiring
public class TestBean
{
    private String message;

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
```

## Custom Tags

The `@Wiring` annotation in the example above gives us another option for the 
XML encoding. Rather than using the `<object>` tag, we can use the name of the 
class itself:

```xml
<TestBean>
    <message>Hello world!</message>
</TestBean>
```

The name can be controlled by passing alternative names to the annotation. For
example, `@Wiring("test-component")` will allow the object to be wired up like
this:

```xml
<test-component>
    <message>Hello world!</message>
</test-component>
```

## Type Tags

Type tags can also be used to specify the data types and create trees of objects.

```xml
<test-component>
    <message>
        <string>Hello world!</string>
    </message>
    <repeat>
        <int>5</int>
    </repeat>
    <subMessages>
        <list>
            <string>First message</string>
            <string><![CDATA[x < y]]></string>
            <json>"Encoding using a\nJSON string"</json>
        </list>
    </subMessages>
</test-component>
```

# Built-In Type Tags

Convirgance (Wiring) supports the following value type tags.

| Tag        | Description|
|------------|------------|
|`<object>`  |Constructs an arbitraty Java object. Must include the attribute `class` to specify the fully qualified class name. The object to instantiate is required to have a default constructor.|
|`<list>`    |Creates an `ArrayList` containing all the values specified inside the tag. Can be used as the root tag of the document if you want to load a collection of items.|
|`<map>`     |Creates a `HashMap`. The only sub-tags allowed are `<entry>` tags specifying the key/value pairs. |
|`<entry>`   |Only allowed inside a `<map>` tag. Must have two sub-tags specifying the key and the value.|
|`<ref>`/`<reference>`|References a value specified elsewhere in the document. The `id` attribute on the `<ref>` tag must match the `id` attribute on the target value. The referenced value may be specified before or after the `<ref>` tag.|
|`<null/>`   | A null value|
|`<string>`  |Identifies the contained data as a `String`. Different encodings of strings, including `CDATA`, will be concatonated to produce the final value.
|`<int>`/`<integer>`|Identifies the contained data as an `int` number|
|`<long>`    |Identifies the contained data as a `long` number|
|`<boolean>` |Identifies the contained data as a `boolean`. Parsed by `Boolean.valueOf()`|
|`<float>`   |Identifies the contained data as a `float` number|
|`<double>`  |Identifies the contained data as a `double` number|
|`<json>`    |String data inside this tag is parsed as JSON and the first value returned. This could be a `JSONObject`, `JSONArray`, or primitve type like `String` or `Integer`|

# Identifier Access

In most cases the root object is the desired object. This can be obtained with the
following code:

```java
var source = new FileSource("configuration.xml");
var root = new XMLWiringParser<Object>(source).getRoot();
```

However, it is sometimes desirable to look up individual objects contained within
the heirarchy. 

For example, let's say the `<message>` tag in `TestBean` had an `id` attribute placed on it:

```xml
<object class="com.test.TestBean">
    <message id="msg">Hello world!</message>
</object>
```

Now we can access the message directly with the identifer:

```java
var source = new FileSource("configuration.xml");
var parser = new XMLWiringParser(source);

String message = (String)parser.get("msg");
```

## Arrays

Wiring has special support for array types like `String[]` or `int[]`. In the
case of a string array, you can pass a comma-separated list:

```xml
<stringArray>One, Two, Three</stringArray>
```

Elements will be automatically trimmed, so only use this approach if your data
is fairly simple, has no commas, and does not care about whitespace.

Arrays in general can be set with the `<list>` tag like this:

```xml
<intArray>
    <list>
        <int>1</int>
        <int>2</int>
        <int>3</int>
    </list>
</intArray>
``` 

In cases of simple data types, this can be simplified with the use of the
`<json>` tag:

```xml
<intArray>
    <json>[1, 2, 3]</json>
</intArray>
``` 

This works because `JSONArray` implements `List` and the array values are parsed
into the expected type. 

## Further Reading

<div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px">
  <span style="display: flex; align-items: center; justify-content: center;font-size:20px; width: 24px; height: 24px">📚</span>
  <a href="https://docs.invirgance.com/javadocs/convirgance-wiring/">JavaDocs: Convirgance (JDBC)</a>
</div>

## Sections

##### [Previous: File Formats](./file-formats?id=file-formats)

##### [Next: OLAP](./olap?id=online-analytical-processing-olap)
