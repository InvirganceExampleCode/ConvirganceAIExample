# Convirgance (JDBC)

Convirgance (JDBC) provides features to simplify database connectivity and bridge
the gaps between the variety of relational databases on the market. It provides automatic
driver downloads, ability to register and remember database connections, a clean object heirarchy for navigating the database 
schema, and an ability to dynamically create queries from database objects.


## Installation

> ![WARNING](images/warning.svg) **<font color="#AA9900">WARNING:</font>**
> Convirgance (JDBC) is in pre-release and may be subject to changes in the APIs

Add the following dependency to your Maven `pom.xml` file:

```xml
<dependency>
    <groupId>com.invirgance</groupId>
    <artifactId>convirgance-jdbc</artifactId>
    <version>0.3.0</version>
</dependency>
```

## Quick Start

Have a connection ```url``` and just need a ```DataSource``` to connect to a 
database? The following code is all you need. Convirgance (JDBC) will 
automatically download the necessary database driver and return you an
configured instance of ```DriverDataSource```.

```java
String url = "jdbc:postgres://my_server/my_database";
String username = "user";
String password = "password";

DataSource source = DriverDataSource.getDataSource(url, username, password);
DBMS dbms = new DBMS(source);
```

## Supported Databases

Convirgance (JDBC) ships with configurations for the following databases:

- Oracle Thin Driver
- Derby Network
- Derby Embedded
- SQL Server (jTDS)
- HSQLDB
- H2
- PostgreSQL
- MariaDB/MySQL
- DB2

All drivers are Type IV drivers, meaning they are pure Java implementations that
require no native libraries or intermediate servers. The names above can be used 
in a program to obtain the desired driver from ```AutomaticDrivers.getDriverByName(<name>)```.


## Automatic Drivers

Convirgance (JDBC) maintains a library of database systems for which it knows
how to retrieve the JDBC driver. Identified drivers are pulled from Maven Central
using Maven's artifcat locator APIs on demand, and thus do not need to ship with your application

For example, this will get you the ```java.sql.Driver``` implementation for PostgreSQL:

```java
AutomaticDriver postgres = AutomaticDrivers.getDriverByName("PostgreSQL");
Driver driver = postgres.getDriver();
```

The database of drivers can be adjusted if you wish to add another database or
modify an existing configuration for your specific needs. The only requirement 
is that you know the Maven artifact coordinates for the driver file(s). 

For example, we may need to access an older version of PostgreSQL that is no longer
compatible with the latest driver. Here we'll create an alternate PostgreSQL 
registration for a 9.2.x driver that we can use as needed:

```java
AutomaticDriver postgres9x = AutomaticDrivers
        .createDriver("PostgreSQL9x")
        .artifact("org.postgresql:postgresql:9.2-1004-jdbc3")
        .driver("org.postgresql.Driver")
        .datasource("org.postgresql.ds. PGSimpleDataSource")
        .build();

// Persist the driver for future use
postgres9x.save();
```


## Stored Connections

Stored connections can be used to create reusable pre-configured database 
connections. The ```url``` or ```javax.sql.DataSource``` values can be configured
once and then the connection can be looked up across restarts of the program.

Here is an example of configuring a driver by ```url```:

```java

AutomaticDriver postgres = AutomaticDrivers.getDriverByName("PostgreSQL");
StoredConnection inventory = postgres
    .createConnection("InventoryDB")
    .driver()
        .url("jdbc:postgresql://localhost:5432/inventory")
        .username("postgres")
    .build();

// Persist the connection information
inventory.save();
```

This version configured the ```javax.sql.DataSource``` parameters
instead:

```java
AutomaticDriver mysql = AutomaticDrivers.getDriverByName("MariaDB/MySQL");
StoredConnection customers = mysql.createConnection("CustomersDB")
                            .datasource()
                                .property("useSSL", true)
                                .property("serverName", "localhost")
                                .property("port", 3306)
                                .property("databaseName", "customers")
                                .property("user", "root")
                                .property("password", "pass")
                            .build();

// Persist the connection information
customers.save();
```

## Navigating Heirarchy

Once we have a configured connection, we can use it to navigate our database's
metadata heirarchy. For example:

```java
StoredConnection customers = StoredConnections.getConnection("CustomersDB");
DatabaseSchemaLayout layout = customers.getSchemaLayout();

System.out.println("Total Catalogs: " + layout.getCatalogs().length);
System.out.println("Current Catalog: " + layout.getCurrentCatalog().getName());
System.out.println("Current Schema: " + layout.getCurrentSchema().getName());
```

Database objects that can be queried implement ```Iterable<JSONObject>```, allowing
them to be used as stream in Convirgance APIs. Note that when a specific
object is requested, a "best match" algorithm is used to handle case sensitivity.
If an object is found that matches the case precisely, it is the item returned. If
the case does not match, the first object returned by the database that matches
a case-insentive compare is returned.

```java
Table type = layout.getCurrentSchema().getTable("CUSTOMER_TYPES");
Table customers = layout.getCurrentSchema().getTable("customers");

// Print data in CUSTOMER_TYPES table
for(var record : type)
{
    System.out.println(record);
}

// Export CUSTOMERS table
new CSVOutput().write(new FileTarget("customers.csv"), customers);

```


## SQL Statements

Accessing all the records in a table is useful, but not ideal under all 
circumstances. Rather than pushing the use of filters in Convirgance (which 
still have to pull all that data for processing), the Convirgance (JDBC) API
allows you to obtain a SQL Query that can be modified with filters, order by,
and other common decorations. 

One the ```SQLStatement``` is modified as needed, it can be transformed into a
Convirgance ```Query``` for execution.

Example:

```java
DatabaseSchemaLayout layout = getLayout();
Table table = layout.getCurrentSchema().getTable("CUSTOMER");

SQLStatement statement = table
                        .select()
                        .column(table.getColumn("name"))
                        .column(table.getColumn("email"), "contact_email")
                        .from(table, "c")
                        .where()
                            .and()
                                .equals(table.getColumn("status"), "active")
                                .greaterThan(table.getColumn("last_order_id"), 8)
                            .end()
                        .done()
                        .order(table.getColumn("name"));

// Ready to use Query object
Query query = statement.query();
```

## Further Reading

<div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px">
  <span style="display: flex; align-items: center; justify-content: center;font-size:20px; width: 24px; height: 24px">📚</span>
  <a href="https://docs.invirgance.com/javadocs/convirgance-jdbc/">JavaDocs: Convirgance (JDBC)</a>
</div>

## Sections

##### [Previous: OLAP](./olap?id=online-analytical-processing-olap)

##### [Next: Web Services](./convirgance-web?id=convirgance-web)
