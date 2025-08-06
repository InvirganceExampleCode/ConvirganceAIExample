# Transforming Data

Transformations let you modify your data as it flows through your application. Think of them as a pipeline where each step can clean, enrich, or reshape your data. Common uses include converting data types (like turning strings into numbers), grouping related records together (similar to SQL GROUP BY), or adding computed fields. This allows you to adapt data from one format or structure to another without loading everything into memory at once.

## Transformers

| Name                       | Description                                                                                                  |
| -------------------------- | ------------------------------------------------------------------------------------------------------------ |
| CoerceStringsTransformer   | Transforms JSONObjects into their actual counterparts, evaluating key values into their real data type.      |
| IdentityTransformer        | A Interface used when creating transformers.                                                                 |
| InsertKeyTransformer       | For replacing or inserting key:value pairs in JSONObjects.                                                   |
| SortedGroupByTransformer   | Used when working with data thats already sorted, grouping similar values on a provided JSON field criteria. |
| Transformer                | The heart of the transformer library, implement this when creating your own.                                 |
| UnsortedGroupByTransformer | The opposite of SortedGroupBy, works on unsorted data and returns the sorted + grouped version.              |

## Transformer Interface

Transformers function by accepting an `Iterable<JSONObject>` source object and
returning a replacement `Iterable<JSONObject>` with the transformation applied.
This allows complex pipelines of transformations to manipulate the stream before
writing the data to its final destination.

```java
Iterable<JSONObject> stream = ...;

// Obtain transformed stream
stream = transformer.transform(stream);

// Transformations can be stacked
stream = anotherTransformer.transform(stream);

```

See also: [Core Concepts > Transformations](concepts.md#transformations)

### Examples

Below are two examples, one goes over the `SortedGroupByTransformer` and the other demonstrates how to use the `Transformer` interface to create an anonymous class.

#### SortedGroupByTransformer Example

The following example showcases joining two tables, and using the `SortedGroupByTransformer` to group the data into a more compact from.

```java
String[] fields = new String[] { "ORDER_ID", "RECIPIENT", "TOTAL", "ITEMS" };
SortedGroupByTransformer sorter = new SortedGroupByTransformer(fields, "lines");
Query query = new Query("SELECT * FROM orders o " +
                        "JOIN order_line l ON l.order_id = o.order_id " +
                        "ORDER BY o.order_id, l.line_id");

Iterable<JSONObject> results = dbms.query(query);
```

And this is what the results from the query would look like.

```json
{"ORDER_ID":1,"TOTAL":54.12,"ITEMS":3,"RECIPIENT":"bob","LINE_ID":1,"PRODUCT":"Fish tank","PRICE":30.00,"QUANTITY":1},
{"ORDER_ID":1,"TOTAL":54.12,"ITEMS":3,"RECIPIENT":"bob","LINE_ID":2,"PRODUCT":"Fish food","PRICE":4.00,"QUANTITY":3},
{"ORDER_ID":1,"TOTAL":54.12,"ITEMS":3,"RECIPIENT":"bob","LINE_ID":3,"PRODUCT":"Fish filter","PRICE":12.12,"QUANTITY":1}
```

Now to sort this we use the `SortedGroupByTransformer` on the results from the query. For unsorted data consider taking a look at [UnsortedGroupByTransformer](https://docs.invirgance.com/javadocs/convirgance/latest/com/invirgance/convirgance/transform/UnsortedGroupByTransformer.html)

```java
Iterable<JSONObject> customerData = sorter.transform(results);
```

The transformer would return the following. In comparison to what the database returned this output is much more concise. Also notice that the duplicate fields were removed.

```json
{
	"RECIPIENT": "bob",
	"TOTAL": 54.12,
	"ORDER_ID": 1,
	"ITEMS": 3,
	"lines": [
		{
			"LINE_ID": 1,
			"PRODUCT": "Fish tank",
			"PRICE": 30,
			"QUANTITY": 1
		},
		{
			"LINE_ID": 2,
			"PRODUCT": "Fish food",
			"PRICE": 4,
			"QUANTITY": 3
		},
		{
			"LINE_ID": 3,
			"PRODUCT": "Fish filter",
			"PRICE": 12.12,
			"QUANTITY": 1
		}
	]
}
```

## Custom Transformers

Transformers that need to update a record as it passes by can be easily created
by implementing the `IdentityTransformer` interface. This interface produces
one `JSONObject` for each `JSONObject` passed in.

Here's an example of a transformer for inserting a timestamp:

```java
Transformer transformer = new IdentityTransformer() {

    @Override
    public JSONObject transform(JSONObject record) throws ConvirganceException
    {
        record.put("timestamp", System.currentTimeMillis());

        return record;
    }
};
```

More complex transformers that change the number of records will have to implement
the `Transformer` interface. The implementation is expected to take an
`Iterator<JSONObject>` and return a new `Iterator<JSONObject>`.

Here's an example of a transformer that duplicates records:

```java
public class DuplicationTransformer implements Transformer
{
    private int count;

    public DuplicationTransformer(int count)
    {
        this.count = count;
    }

    @Override
    public Iterator<JSONObject> transform(Iterator<JSONObject> iterator)
    {
        return new Iterator<JSONObject>() {

            private JSONObject record;
            private int counter;

            @Override
            public boolean hasNext()
            {
                return (record != null || iterator.hasNext());
            }

            @Override
            public JSONObject next()
            {
                JSONObject result;

                if(record != null)
                {
                    result = new JSONObject(record);

                    if(--counter < 1) record = null;

                    return result;
                }

                record = iterator.next();
                counter = count;

                return new JSONObject(record);
            }
        };
    }
}
```

## Best Practices

- Avoid `ArrayList` and other buffers in your transformer. Memory buffers can cause `OutOfMemoryError` exceptions that crash your program.
- Use `CoerceStringsTransformer` to standardize data types before processing.
- Leverage `SortedGroupByTransformer` for efficient grouping when working with pre-sorted data.
- Apply `UnsortedGroupByTransformer` when sorting is not guaranteed but grouping is required.
- Combine transformers to build complex transformation pipelines.

## Further Reading

<div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px">
  <span style="display: flex; align-items: center; justify-content: center;font-size:20px; width: 24px; height: 24px">📚</span>
  <a href="https://docs.invirgance.com/javadocs/convirgance/latest/com/invirgance/convirgance/transform/package-summary.html">JavaDocs: Transformers</a>
</div>

## Sections

##### [Previous: Filtering Data](./filtering-data?id=filters)

##### [Next: File Formats](./file-formats?id=file-formats)
