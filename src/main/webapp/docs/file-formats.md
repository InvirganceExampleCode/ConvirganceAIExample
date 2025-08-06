# File Formats

Convirgance treats all data sources equally, whether they're CSV files, JSON documents, or database tables. You can read from one format and write to another without changing your business logic. Need to take a CSV file and output JSON? Or query a database and save to CSV? The same filtering and transformation rules work regardless of your input or output format, making it easy to work with data in whatever form it arrives or needs to be delivered.

## Supported Formats

| Format         | Description                                    | Read/Write | Extensions     |
| -------------- | ---------------------------------------------- | ---------- | -------------- |
| CSV            | Comma-separated values for tabular data        | Read/Write | `.csv`         |
| Pipe Delimited | Column data separated by pipes                 | Read/Write | `.txt` ,`.psv` |
| Tab Delimited  | Column data separated by tabs                  | Read/Write | `.txt` ,`.tsv` |
| Delimited      | Custom delimiter-separated data                | Read/Write | `.txt`         |
| JSON           | JavaScript Object Notation for structured data | Read/Write | `.json`        |
| JBIN           | Binary JSON format                             | Read/Write | `.bin`,`.jbin` |

## Input and Output

The `Input` and `Output` interfaces are used to read/write from an `Iterable` stream to a `Object` implementing `Source` or `Target` respectively.

## Example: Global Supply Chain Integration

Global Logistics Corp needs to integrate data from multiple suppliers, warehouses, and shipping partners. Each partner provides data in different formats, requiring a **flexible** system that can **read and write** with a **variety** of **file-sources**.

### Scenario 1: Processing Supplier Inventory (Delimited Files)

The company's European supplier sends inventory updates as pipe-delimited files. Here's how we convert them into JSON to use with our Warehouse management system:

```java
FileSource source = new FileSource("europe_inventory.txt");

DelimitedInput input = new DelimitedInput();
Iterable<JSONObject> records = input.read(source);

JSONOutput output = new JSONOutput();
FileTarget inventory = new FileTarget("warehouse_status.json");
output.write(inventory, records);
```

This input file looks like:

```
product|quantity
Laptop|300
```

And here is the converted file, `warehouse_status.json`

```json
{ "product": "Laptop", "quantity": 300 }
```

### Scenario 2: Warehouse Management (JSON)

Our warehouse management system uses JSON for real-time updates. Here are a few examples covering the implementation for reading and writing JSON.

#### Reading Warehouse Data

```java
FileSource source = new FileSource("warehouse_status.json");
Iterable<JSONObject> input = new JSONInput().read(source);

for(JSONObject warehouse : input)
{
    System.out.println(warehouse);
}
```

Output shows:

```json
{ "location": "Chicago", "capacity": 3000, "utilization": 85 }
```

#### Writing Warehouse Data

The warehouse management system provides an export function, its implementation might look like.

```java
DBMS database = new DBMS(source);
Query status = new Query("SELECT * FROM warehouse_status");
Iterable<JSONObject> results = database.query(status);

FileTarget target = new FileTarget("status_update.json");
JSONOutput out = new JSONOutput();

out.write(target, results);
```

### Scenario 3: Shipping Reports (CSV)

For executive dashboards, we convert warehouse data to CSV format:

```java
FileSource warehouse = new FileSource("warehouse_data.json");
Iterable<JSONObject> data = new JSONInput().read(warehouse);

FileTarget report = new FileTarget("shipping_report.csv");
CSVOutput output = new CSVOutput();

output.write(report, data);
```

This generates:

```csv
location, capacity, utilization
Chicago, 3000, 85
```

### Scenario 4: High-Performance Data Exchange (JBIN)

For high-frequency updates between distribution centers, we use JBIN format:

```java
FileSource warehouse = new FileSource("warehouse_data.json");
Iterable<JSONObject> distribution = new JSONInput().read(warehouse);

ByteArrayTarget target = new ByteArrayTarget();
JBINOutput output = new JBINOutput();

output.write(target, distribution);
```

<!-- TODO update this example when we have something like URLOut -->

This approach has helped Global Logistics Corp **reduce data processing time** by a measurable amount while also **maintaining compatibility** with all their partners' systems.

## The Input/Output Interfaces

Convirgance provides an extensible input/output interface to allow you to integrate support for new data formats by defining custom readers and writers.

### Example: Properties File

Lets go over adding support for the `.properties` file-type.

#### PropertiesInput Implementation

Here is the basic implementation of `Input` for our `.properties` file.

```java
// An `Input` to handle reading in a source containing the stream for some .properties file
Input<JSONObject> input = new Input<JSONObject>() {

    @Override
    public InputCursor<JSONObject> read(Source source)
    {
        return new InputCursor<JSONObject>() {
            private final BufferedReader reader = new BufferedReader(new InputStreamReader(source.getInputStream()));

            @Override
            public CloseableIterator<JSONObject> iterator()
            {
                return new CloseableIterator<JSONObject>() {
                    String nextLine;

                    {
                        try
                        {
                            nextLine = reader.readLine();
                        }
                        catch (IOException exception)
                        {
                            throw new ConvirganceException(exception);
                        }
                    }

                    @Override
                    public boolean hasNext()
                    {
                        return nextLine != null;
                    }

                    @Override
                    public JSONObject next()
                    {
                        String[] property = nextLine.split("=", 2);
                        JSONObject obj = new JSONObject();

                        try
                        {
                            nextLine = reader.readLine();
                        }
                        catch (IOException exception)
                        {
                            throw new ConvirganceException(exception);
                        }

                        obj.put(property[0], property[1]);
                        return obj;
                    }

                    @Override
                    public void close() throws IOException
                    {
                        reader.close();
                    }
                };
            }
        };
    }
};
```

#### PropertiesOutput Implementation

Here is the basic implementation of `Output` for our `.properties` file.

```java
/**
 * This is used to write out JSONObjects to a .properties files.
 */
Output propertiesOutput = new Output() {

    @Override
    public OutputCursor write(Target target)
    {
        return new OutputCursor() {
            private final PrintWriter writer = new PrintWriter(target.getOutputStream(), false);

            @Override
            public void write(JSONObject record)
            {
                Object value;

                for (String key : record.keySet())
                {
                    value = record.get(key);

                    writer.print(key);
                    writer.print("=");
                    writer.print(value.toString().replace("\n", ""));
                    writer.print("\n");
                }
            }

            @Override
            public void close()
            {
                writer.close();
            }
        };
    }

    @Override
    public String getContentType()
    {
        return "text/properties";
    }
};
```

### Using the Properties Implementation

#### Reading properties values from a Database

In the following example we are going to use our new `.properties` implementation to write the results of a query to a file. We will be using the `propertiesOutput` from the previous example.

Database Data:

| blending_mode | accuracy | model    |
| ------------- | -------- | -------- |
| lighten       | 0.75     | photopea |

```java
DBMS dbms = new DBMS(source);
Query query = new Query("select blending_mode, accuracy, model from SETTINGS limit 1");

Iterable<JSONObject> results = dbms.query(query);
FileTarget target = new FileTarget("user.properties");


propertiesOutput.write(target, results);

/*
# user.properties would contain the following info (based on the data stored in the database)

blending_mode=lighten
accuracy=0.75
model=photopea
*/
```

### Best Practices

- Ensure you're following the file-type specifications, created by [IETF](https://www.ietf.org/).
- Robust error handling to deal with malformed files.
- Optimize performance for large files by using streaming approaches where applicable.

## Further Reading

<div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px">
  <span style="display: flex; align-items: center; justify-content: center;font-size:20px; width: 24px; height: 24px">📚</span>
  <a href="https://docs.invirgance.com/javadocs/convirgance/latest/com/invirgance/convirgance/input/package-summary.html">JavaDocs: File Formats</a>
</div>

## Sections

##### [Previous: Transforming Data](./transforming-data?id=transforming-data)

##### [Next: OLAP Tools](./olap?id=online-analytical-processing-olap)
