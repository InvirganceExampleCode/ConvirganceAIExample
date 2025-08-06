# Database Operations

Convirgance provides a unified method of working with SQL database management
systems (DBMS) while maintaining both [ACID compliance](https://en.wikipedia.org/wiki/ACID)
and industry-leading performance.

Rather than dealing with low-level JDBC code, you can work with intuitive
concepts like atomic operations (where everything either succeeds or fails together),
batch processing for better performance, and simple querying that returns
easy-to-use JSON objects. This gives you the control of direct SQL with
the safety of managed transactions.

## Database Connections

Database connectivity is dependent on the modern `javax.sql.DataSource` approach
to database connections. Database-specific `DataSource` implementations can be
initialized and configured with server information and credentials. Once this is
done, the `DataSource` becomes a source for obtaining connections.

Most modern systems provide a method of configuring `DataSource` objects for use
by the application. Common approaches include:

- JNDI registration in Application Servers (Glassfish, Tomcat, JBoss, etc.)
- Spring Boot [Data Access](https://docs.spring.io/spring-boot/how-to/data-access.html)
- Connection Pool configuration (e.g. [Apache DBCP](https://commons.apache.org/proper/commons-dbcp/))
- Manual initialization of database-specific `DataSource` implementation

Consult the documentation for your server or framework for more on how
to configure and obtain a `DataSource` instance.

## DBMS API

Central to database access is the `DBMS` object. Simply pass your `DataSource`
to the constructor and you are ready to work with the database.

```java
DataSource source = ...;
DBMS dbms = new DBMS(source);
```

Now we are ready to use the DBMS APIs for querying and inserting/updating data.

## Querying

SQL queries in Convirgance are wrapped by the `Query` object. This object is
responsible for parsing the SQL to detect dynamic bind variables, then allowing
the values to be set.

<!-- TODO The wording here seems odd, maybe 'allowing values to be bound later on'  -->

The prepared `Query` is then passed to the `DBMS` to obtain an `Iterable<JSONObject>`
stream that is compatible with Convirgance's APIs. Here is an example of a
basic, un-parameterized query:

```java
Query query = new Query("select * from CUSTOMER");

for(JSONObject record : dbms.query(query))
{
    // Pretty print the database record
    System.out.println(record.toString(4));
}
```

### Parameter Binding

Convirgance uses named parameters (prefixed with `:`) to
safely bind values into SQL queries. Values can be set directly on `Query` or
bound in bulk using a `JSONObject`.

The use of named variables eliminates the need to count and align traditional
`?` placeholders. For example, `:userId` in your query would match with the
`userId` field in your binding object. Multiple occurrences of `:userId` in the
SQL would all bind to the same value.

```java
Query query = new Query("SELECT * FROM customer " +
                        "WHERE DISCOUNT_CODE = :membershipType " +
                        "AND STATE = :state");

// Set the bind variables
query.setBinding("membershipType", "G");
query.setBinding("state", "CA");

for(JSONObject record : dbms.query(query))
{
    // Pretty print the database record
    System.out.println(record.toString(4));
}
```

## Transactions

Database updates such as inserts, updates, and administrative calls to stored
procedures are all handled in a transaction context. This ensures that the
updates are applied with atomicity, consistency, isolation, and durability (ACID)
to the limits provided by the database management system.

The work that a needs to be accomplished in an update is captured by the
`AtomicOperation` interface. Implementations of the interface are given a
JDBC connection to the database and asked to perform their updates.

Some common implementations include:

- `QueryOperation` - Wraps a `Query` object to run the query as a transaction
- `BatchOperation` - Performs JDBC batch updates for bulk inserts and updates
- `TransactionOperation` - Allows bundling of numerous other operations into a single transaction

### Query Transactions

The most basic type of database update is a simple insert or update.
`QueryOperation` supports this use case by wrapping a `Query` and running
the query with all the features of `Query` including bind variables.

```java
String sql = "insert into CUSTOMER values (:id, :name, :devices, :pets)";
Query insert = new Query(sql);
QueryOperation operation = new QueryOperation(insert);

// Bind variables
insert.setBinding("id", 5);
insert.setBinding("name", "BubbaG");
insert.setBinding("devices", 3);
insert.setBinding("pets", 3);

// Execute the insert
dbms.update(operation);
```

### Bulk Inserts and Updates

It is common to want to load numerous records into the database at once rather
then executing individual insert/updates. `BatchOperation` can be configured
with a `Query` that will be used as a prepared statement for JDBC bulk inserts.
The `BatchOperation` can then be fed an `Iterable<JSONObject>` stream that it
will use as a source of records for the bulk insert/updates.

For example, let's say we have a file called `data.json` that has data we wish to bulk load:

```json
[
	{ "id": 1, "name": "John", "devices": 3, "pets": 1 },
	{ "id": 2, "name": "Bob", "devices": 1, "pets": 2 },
	{ "id": 3, "name": "Kyle", "devices": 1, "pets": 10 },
	{ "id": 4, "name": "Larry", "devices": 0, "pets": 0 },
	{ "id": 5, "name": "Bubba", "devices": 3, "pets": 3 }
]
```

The following code will load the above `data.json` and insert it into the `CUSTOMER` table:

```java
// Obtain a stream of JSON data
Iterable<JSONObject> stream = new JSONInput().read(new FileSource("data.json"));

// Setup our query for each insert
String sql = "insert into CUSTOMER values (:id, :name, :devices, :pets)";
Query insert = new Query(sql);

// Execute the bulk load
dbms.update(new BatchOperation(insert, stream));

```

Note how the bind keys in the SQL match the key names in the JSON. This
allows `BatchOperation` to bind each record to the query as it streams the
data for load.

_Warning: `BatchOperation` can lead to partial commits if the number of records
loaded exceeds the auto commit limit. The auto commit limit exists to
prevent the database transaction log from filling up and failing the load. This
limit defaults to 1,000 records and can be adjusted to support database tuning._

### Batched Transaction

The `TransactionOperation` allows multiple operations to be queued and then
executed sequentially. If an error occurs in any of the batched operations,
the changes from all operations will be rolled back.

```java
Query delete = new Query("delete from CUSTOMER where id = 5");
Query insert = new Query("insert into CUSTOMER values (5, 'Bubba', 0, 7)");

// Setup a transaction to run delete and insert in order
TransactionOperation transaction = new TransactionOperation(delete, insert);

// Execute the transaction
dbms.update(transaction);
```

## Best Practices

- Use `TransactionOperation` for multiple operations and to ensure atomicity incase issues arise.
- Leverage `BatchOperation` for large-scale operations to optimize performance.
- Using interval commits to avoid overflowing the transaction buffer
- Utilize named bindings as they ensure the correct JSONObject values will be used.

## Further Reading

<div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px">
  <span style="display: flex; align-items: center; justify-content: center;font-size:20px; width: 24px; height: 24px">📚</span>
  <a href="https://docs.invirgance.com/javadocs/convirgance/latest/com/invirgance/convirgance/dbms/package-summary.html">JavaDocs: DBMS</a>
</div>

## Sections

##### [Previous: Core Concepts](./concepts?id=core-concepts)

##### [Next: Filtering Data](./filtering-data?id=filters)
