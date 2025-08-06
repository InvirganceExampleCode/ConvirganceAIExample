<style>body {text-align: left}</style>

# Online Analytical Processing (OLAP)

As businesses grow and expand, they gain useful analytical data about
their own customers and the market itself. Online Analytical Processing, or OLAP,
provides a means of sythesizing this data into useful measures to provide business intelligence insights.

Convirgance (OLAP) provides an easy and intuitive
way to build OLAP tools around Star Schemas to produce SQL queries and perform
data analysis to aid businesses intelligence, without the need for expensive
outsourcing.

A star schema is a type of multidimensional database design commonly used in data warehousing,
where a central fact table (containing quantitative data) is connected to multiple
dimension tables (describing the context of the data). This structure simplifies analytical queries and accelerates
information retrieval from databases.

> ![WARNING](images/warning.svg) **<font color="#AA9900">WARNING:</font>**
> Convirgance (OLAP) is in pre-release and may be subject to change

## Installation

Add the following dependency to your Maven `pom.xml` file:

```xml
<dependency>
    <groupId>com.invirgance</groupId>
    <artifactId>convirgance-olap</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Scenario

Suppose you are working on an online retail analytics and you have the following
star schema model with ORDERS as the central fact table, and PRODUCTS,
SALESPERSONS, CUSTOMERS, and DAYS as dimensions:

![alt text](images/starschema.svg)

The eventual goal of this exercise is to create SQL queries from OLAP structure.
Read along to better understand how Convirgance (OLAP) offers such functionality.

## Representing Database Structure

<!--
| Class          | Function                                                                                          |
| -------------- | ------------------------------------------------------------------------------------------------- |
| `Database`     | Database structure representation; contains table objects.                   |
| `Table`        | For Table object representation. Contains a primary key and list of foreign keys, which are columns shared between a source and target table.      |
| `ForeignKey`   | Captures the connection between a source table, a column on the source table, and a target table. |
| `SQLGenerator` | Support for creating and outputting SQL queries for working with OLAP.                   |

-->

You can use Convirgance (OLAP) to first capture the database structure with the five tables:

```java
Database stardb = new Database("StarDB");

Table orders = new Table("ORDERS", "order_id"); // "order_id" is the primary key for ORDERS table
Table products = new Table("PRODUCTS", "product_id");
Table salespersons = new Table("SALESPERSONS", "salesperson_id");
Table customers = new Table("CUSTOMERS", "customer_id");
Table days = new Table("DAYS", "day_id");

stardb.addTable(orders);
stardb.addTable(products);
stardb.addTable(salespersons);
stardb.addTable(customers);
stardb.addTable(days);
```

You can then capture the relationship between the ORDERS table and the rest of
the tables by adding the foreign keys. The ORDERS table will
become the table following the `FROM` command in SQL queries.

```java
orders.addForeignKey("product_id", products);
orders.addForeignKey("salesperson_id", salespersons);
orders.addForeignKey("customer_id", customers);
orders.addForeignKey("day_id", days);
```

## Generating SQL Queries

Now that we have our database representation, we can use the SQLGenerator to
output OLAP queries. Here is how it works:

<!--
table just represents that a table exists.
an object in star schema.
on top of that we put dimensions/metrics

todo:
1. just say we create a structure for the star schema
2. once we have the structure, we can specify the dimensions and metrics
3. create a super simple star schema
    star diagram (e.g. 4 dimensions, 1 central table)

Explain with the example the schema, dimensions, facts (metrics)
Fact table at the centre is the table containing all the facts/metrics.
Only have a few records (4-10).
-->

This java code...

```java
SQLGenerator generator = new SQLGenerator();

generator.addTable(orders);
generator.addSelect("product_name", products);
generator.addSelect("salesperson_name", salespersons);
generator.addAggregate("sum", "cost_total", orders, "cost_dollars");

generator.getSQL(); // Get the SQL!
```

...generates the following SQL query:

```SQL
select
    PRODUCTS.product_name,
    SALESPERSONS.salesperson_name,
    sum(ORDERS.cost_dollars) as cost_total,
from ORDERS
join PRODUCTS on PRODUCTS.product_id = ORDERS.product_id
join SALESPERSONS on SALESPERSONS.salesperson_id = ORDERS.salesperson_id
group by
    PRODUCTS.product_name,
    SALESPERSONS.salesperson_name
```

1. Adding ORDERS table to the generator first sets it as the central fact table
   and handles the joins between ORDERS and other tables.

2. The `addSelect(String Column, Table table)` method lets us specify which column to
   select from table. It relies on previously specified primary and foreign keys to
   handle the joins.

3. The `addAggregate(String function, String column, Table table, String alias)` method
   allows you to aggregate a metric from the central table into a measure. The first
   parameter is a SQL function dictates the nature of
   aggregation. You can optionally specify the alias for the measure by passing
   the fourth parameter to the method.

Using the `SQLGenerator` to generate SQL queries in this way is simple; yet, not ideal.
The SQL Generator does not actually
capture the Dimensional analysis needed for OLAP. We want to explicitly define Dimensions,
Metrics, and Measures and let the user select them for interactive analysis.

Luckily, Convirgance (OLAP) introduces that additional layer for OLAP Interface. Read on to see how it works.

<!--
1.  The `addSelect(String column, Table table)` lets us define a select from a table
2.  We capture this select in a `Column` inner class that represents the column and table
3.  We add the table to a list of tables we need to create joins between
4.  The addTable(Table table) method is public because OLAP queries are centered around a Fact table. The Fact table must be added first as the “From” table so that all joins fan out from it. Even if we never select any data from the Fact table itself.
5.  The getSQL() method loops through the select list to generate the columns to select and generates the first table in the list as the “from” table. It then calls generateJoins(from) and generateGroupBy().
<!-- TODO nit: 4 and 5 are a little on the long side
6.  generateJoins(from) matches the ForeignKeys in the “from” table to tables in our list of selects and generates a join for each one
7.  generateGroupBy() loops through the selected columns again and generates a group by list of all columns that are not aggregates. i.e. They are dimension columns.

    - Wait… what are aggregates?

8.  Aggregates are the “fact” or “metric” columns that we want to roll up using a function like “sum()” or “avg()”
<!-- TODO nit 4, 5, 6, 8: use backticks when referring to the From table also with functions/objects table
9.  We can call `addAggregate(String function, String column, Table table)` to add the aggregate to our select list
10. We use an inner class called Aggregate to capture this select. Aggregate uses the inheritance pattern in OOP to “be” a Column while additionally capturing the function and overriding the getSQL() method.

    - Because our Aggregate “is” a Column, we don’t need to change any of our select logic or manage it separately.

    <!-- TODO nit: use single quotes when referring to something abstract ex nuclear fission reactors 'existed' 2 billion years ago but only because of the earths enviroment and very specific conditions. -->

## OLAP Interface

To bridge the database representation with an OLAP interface, we have Dimensions
and Measures to work with as part of the Star Schema.

#### Defining Dimensions and Measures

Suppose we want to add the product name as one of the dimensions in our analysis,
and remember that we have the `products` table defined above. Further suppose
that the product names are contained in the `product_name`
column in the `products ` table. We can create a `Dimension` object to represent this dimension as follows:

```java
Dimension productName = new Dimension("Product Name", products, "product_name");
```

The first parameter passed to the constructor above is just what we want to name our
dimension. The second parameter is the table and the third is the column in the
table from which we pull our dimensional information.

Here is how we can define all Dimensions we might want to reference later

```java
Dimension productName = new Dimension("Product Name", products, "product_name");
Dimension salespersonName = new Dimension("Salesperson Name", salespersons, "salesperson_name");
// Dimension anotherDimension = new Dimension(<Dimension Name>, table, <column_name>);
// Repeat for any dimensions you want to define
```

It's a similar approach with Measures, which are aggregates over Metrics. We first
want to create a Metric object by specifying the table and the column that contains
quantitative data. Suppose we want to look at the cost of an order. Remember we have
our `orders` table already defined, and suppose it contains the `cost_dollars` column.
We define the Metric as follows:

```java
Metric cost = new Metric(orders, "cost_dollars");
```

Now that we have the metric, we can define a measure over it, by specifying the
Measure's name, the Metric object, and the SQL function that aggregates the metric.
Here is an example that creates a Measure for total cost:

```java
Measure costTotal = new Measure("Total", cost, "sum");
```

Of course, we can combine the creation of a Measure and Metric into a one-liner and
similarly define all
the measures we might want to reference later:

```java
Measure costTotal = new Measure("Total", new Metric(orders, "cost_dollars"), "sum");
// Measure anotherMeasure = new Measure(<name>, metric, <SQL function>);
// Repeat for any measures
```

#### Defining a Star object to represent the Star Schema

Now that we have our Dimensions and Measures, we can add them to the star object to complete the schema:
Remember that the ORDERS table is at the center of our star schema. We thus want to construct
our star object as such:

```java
Star star = new Star(orders); // sets ORDERS table as the central fact table
```

Now we can add the dimensions and measures we defined above to our star object:

```java
star.addDimension(productName);
star.addDimension(salespersonName);
// Add any other dimensions

star.addMeasure(costTotal);
// Add any other measures
```

#### Creating a Report Generator to generate SQL

We now have the star object that reflects the star schema introduced at the beginning
of this exercise. We can now generate the same SQL queries by specifying existing
dimensions and measures to our `ReportGenerator` object:

```java
ReportGenerator generator = new ReportGenerator(star); // First need to assign the generator to the star schema.

// Dimensions and Metrics must come from the star object assigned to the generator.
generator.addDimension(star.getDimension("Product Name"));
generator.addDimension(star.getDimension("Salesperson Name"));
generator.addMeasure(star.getMeasure("Total"));

generator.getSQL(); // Generate the SQL!
```

That's it! Once we have the Star object, we can simply use the ReportGenerator
for our analytics. Of course, defining all these dimensions and measures for the star
can be tedious, but fortunately we can use the Spring Configuration file to quickly
import our star. Move on to the next session to see an example.

<!--


| Class             | Function                                                                     |
| ----------------- | ---------------------------------------------------------------------------- |
| `Dimension`       | Provides support for qualitative/contextual descriptions of data.            |
| `Measure`         | Provides support for aggregated quantitative values of data.                 |
| `Metric`          | Provides support for quantitative values of data.                            |
| `ReportGenerator` | Provides support for the SQL query generation from constructed star schemas. |
| `Star`            | Provides support for the central star schema.                                |

1. The `Star` plays the role of a central container, and it tracks the fact table at the center.
   Around the fact table are Dimensions. Inside the fact table are Metrics.
   Metrics will be aggregated in queries, so these are represented by Measures.

2. `Dimension` and `Metric` are simple, they simply contain a column
   name and a Table reference.

3. `Measure` also contains a column name, but no table. Instead, it wraps the
   associated with it Metric along with a function. The function is just the name of the
   SQL function we want to apple, such as "avg" or "sum".

4. Finally, the `ReportGenerator` allows to create queries within OLAP context.
   As an OLAP user, you no longer need to think about the underlying query structure. Instead,
   you simply specify Dimensions and Measures, and the OLAP layer takes care of the rest by pushing
   the the parameters down to the SQLGenerator.

  <!-- TODO nit: order list as items appear in the table or reoder the table -->

<!--
Here too, you can add the Star schema to the Spring configuration file just like we did with the Database above.
You can then lookup the Star from the configuration file and query immediately:

Here's how we can generate the same SQL Query as above by using the Star configured in Spring:

<!-- fill in the story with code actually constructing a star.
There is a test case that constructs a star that you can use. -->

<!--As a next step, you can use [Convirgance-WEB](https://github.com/InvirganceOpenSource/convirgance-web) to create web services around this OLAP
structure. -->

## Spring Method

Rather than manually defining all tables, dimensions, and measures for our star
schema, we can have a single configuration file to pull from. Follow along to see
how we can setup and use a Spring configuration file for our star schema to generate SQL.

Let's first configure the four tables surrounding our central ORDERS table. This might be a
good time to remind ourselves what our star schema looked like. We define the tables
in Spring as follows:

```xml
<bean id="products" class="com.invirgance.convirgance.olap.sql.Table">
    <property name="name" value="PRODUCTS"/>
    <property name="primaryKey" value="product_id"/>
    <property name="foreignKeys">
        <list></list>
    </property>
</bean>

<bean id="salespersons" class="com.invirgance.convirgance.olap.sql.Table">
    <property name="name" value="SALESPERSONS"/>
    <property name="primaryKey" value="salesperson_id"/>
    <property name="foreignKeys">
        <list></list>
    </property>
</bean>

<bean id="customers" class="com.invirgance.convirgance.olap.sql.Table">
    <property name="name" value="CUSTOMERS"/>
    <property name="primaryKey" value="customer_id"/>
    <property name="foreignKeys">
        <list></list>
    </property>
</bean>

<bean id="days" class="com.invirgance.convirgance.olap.sql.Table">
    <property name="name" value="DAYS"/>
    <property name="primaryKey" value="day_id"/>
    <property name="foreignKeys">
        <list></list>
    </property>
</bean>
```

Next step is to configure our central fact table (ORDERS) with foreign keys referencing
each of the four tables defined above:

```xml
<bean id="orders" class="com.invirgance.convirgance.olap.sql.Table">
    <property name="name" value="ORDERS"/>
    <property name="primaryKey" value="order_id"/>
    <property name="foreignKeys">
        <list>
            <bean class="com.invirgance.convirgance.olap.sql.ForeignKey">
                <property name="sourceKey" value="product_id" />
                <property name="target">
                    <ref bean="products" />
                </property>
            </bean>
            <bean class="com.invirgance.convirgance.olap.sql.ForeignKey">
                <property name="sourceKey" value="salesperson_id" />
                <property name="target">
                    <ref bean="salespersons" />
                </property>
            </bean>
            <bean class="com.invirgance.convirgance.olap.sql.ForeignKey">
                <property name="sourceKey" value="customer_id" />
                <property name="target">
                    <ref bean="customers" />
                </property>
            </bean>
            <bean class="com.invirgance.convirgance.olap.sql.ForeignKey">
                <property name="sourceKey" value="day_id" />
                <property name="target">
                    <ref bean="days" />
                </property>
            </bean>
        </list>
    </property>
</bean>
```

Now that we have the tables, we can configure the star with the relevant
dimensions and measures:

```xml
<bean class="com.invirgance.convirgance.olap.Star">
    <property name="fact">
        <ref bean="orders"/>
    </property>
    <property name="dimensions">
        <list>
            <bean class="com.invirgance.convirgance.olap.Dimension">
                <property name="name" value="Product Name"/>
                <property name="column" value="product_name"/>
                <property name="table">
                    <ref bean="products"/>
                </property>
            </bean>
            <bean class="com.invirgance.convirgance.olap.Dimension">
                <property name="name" value="Salesperson Name"/>
                <property name="column" value="salesperson_name"/>
                <property name="table">
                    <ref bean="salespersons"/>
                </property>
            </bean>
            <!-- Other dimensions defined here -->
        </list>
    </property>
    <property name="measures">
        <list>
            <bean class="com.invirgance.convirgance.olap.measures.SumMeasure">
                <property name="name" value="Total"/>
                <property name="metric">
                    <bean class="com.invirgance.convirgance.olap.Metric">
                        <property name="column" value="cost_dollars"/>
                        <property name="table">
                            <ref bean="orders"/>
                        </property>
                    </bean>
                </property>
            </bean>
            <!-- Other measures defined here -->
        </list>
    </property>
</bean>
```

That is all for the Spring file. Now we simply import the Star into java and use
the report generator in the same way as we did in the previous section:

```java
Star star = getStar() // Load the Star from the Spring file

ReportGenerator generator = new ReportGenerator(star);

generator.addDimension(star.getDimension("Product Name"));
generator.addDimension(star.getDimension("Salesperson Name"));
generator.addMeasure(star.getMeasure("Total"));

generator.getSQL(); // Generate the SQL!
```

<!-- TODO maybe add an example for this

use the example above, create the spring config file based off that.

-->

## Further Reading

<div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px">
  <span style="display: flex; align-items: center; justify-content: center;font-size:20px; width: 24px; height: 24px">📚</span>
  <a href="https://docs.invirgance.com/javadocs/convirgance-olap/latest/index.html">JavaDocs: OLAP</a>
</div>

## Sections

##### [Previous: File Formats](./file-formats?id=file-formats)

##### [Convirgance JDBC](./convirgance-jdbc?id=convirgance-jdbc)
