# Filters

Filters provide SQL-like operations for working with data streams. You can use familiar concepts like equals, greater than, less than, and combine them with AND/OR operations - just like you would in a SQL WHERE clause. This gives you the power of SQL filtering even when working with non-SQL data sources. For example, you can easily filter records where age > 18 AND status = 'active', or find all users who are either admins OR moderators.

<!-- TODO add Like, Contains -->

## Types of Filters

| Symbol   | Description                                         |
| -------- | --------------------------------------------------- |
| `Filter` | Base interface for filter evaluation and conditions |
| `&&`     | Combines filters with AND logic                     |
| `\|\|`   | Combines filters with OR logic                      |
| `!=`     | Inverts filter result                               |
| `==`     | Coercive equality comparison                        |
| `===`    | Strict equality comparison                          |
| `>`      | Greater than comparison                             |
| `>=`     | Greater than or equal comparison                    |
| `<`      | Less than comparison                                |
| `<=`     | Less than or equal comparison                       |

## Examples

### Basic Greater Than

The below filter returns true because the rider's height is above 122(cm).

```java
String key = "height";
int value = 122;

JSONObject rider = new JSONObject("{\"height\": 152}");
return new GreaterThanFilter(key, value).test(rider);
```

## Interface Examples

### Filter

The `Filter` interface extends `Transformer`, allowing you to quickly create filters to use on groups of `JSONObjects`.

```java
FileSource source = new FileSource("client_data.json");
Iterator<JSONObject> records = new JSONInput().read(source).iterator();

String key = "name";
String find = "Smith";

Filter nameFilter = new Filter() {

    @Override
    public boolean test(JSONObject record)
    {
        return record.getString(key).contains(find);
    }
};

Iterator<JSONObject> filtered = nameFilter.transform(records);
```

### Comparative Filtering

The below example uses a custom `ComparatorFilter` to collect JSONObjects that are considered 'old'.

```java
Iterable<JSONObject> current;
Iterator updated;

DBMS database = new DBMS(source);

String search = "Select last_update FROM customer";

ComparatorFilter date = new ComparatorFilter() {

    @Override
    public boolean test(JSONObject record)
    {
        String key = this.getKey();
        Object value = this.getValue();

        Date compare = new SimpleDateFormat("yyyy-MM-dd").parse(record.getString(key));
        Date test = new SimpleDateFormat("yyyy-MM-dd").parse(value.toString());

        return test.after(compare);
    }
};

date.setKey("last_update");
date.setValue("2007-01-01");

current = database.query(new Query(search));

updated = date.transform(current.iterator());
```

## Best Practices

- Combine filters with `AndFilter` and `OrFilter` to define complex filtering criteria.
- Use `NotFilter` to filter records using the falsy evaluation of other `Filter`(s).
- When working with numeric or comparable fields, leverage `CoerceStringsTransformer` to make sure the records values are the correct data-type.

## Further Reading

<div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px">
  <span style="display: flex; align-items: center; justify-content: center;font-size:20px; width: 24px; height: 24px">📚</span>
  <a href="https://docs.invirgance.com/javadocs/convirgance/latest/com/invirgance/convirgance/transform/filter/package-summary.html">JavaDocs: Filters</a>
</div>

## Sections

##### [Previous: Database Operations](./database-operations?id=database-operations)

##### [Next: Transforming Data](./transforming-data?id=transforming-data)
