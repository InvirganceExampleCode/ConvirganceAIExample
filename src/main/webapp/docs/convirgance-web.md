# Convirgance (Web Services)

Convirgance (Web Services) builds upon the Convirgance platform by offering a low-code/no-code solution for building web services.

The Web Services API is built upn the Jakata / Java EE standards, making it highly compatible with existing applications.

## Installation

> ![WARNING](images/warning.svg) **<font color="#AA9900">WARNING:</font>**
> Convirgance (Web Services) is in pre-release and may be subject to change

Add the following dependency to your Maven `pom.xml` file:

```xml
<dependency>
    <groupId>com.invirgance</groupId>
    <artifactId>convirgance-web</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Core Architecture

### Services

Services are the central components; serving as entry points for HTTP requests and orchestrating the data processing workflow.

#### SelectService

The `SelectService` is designed for data retrieval operations, used for implementing GET endpoints. Following a pipeline approach:

- Extract parameters from the HTTP request
- Use those parameters to retrieve data through configured bindings
- Apply optional transformations to the data
- Format and return the results using a configured output formatter

This service can be used for creating search interfaces, data listing, and any operation that retrieves information.

#### InsertService

The `InsertService` handles data submission operations, used for implementing POST endpoints. It follows a different pipeline:

- Extract raw data from the HTTP request using an Origin component
- Parse the data with a configured Input processor
- Apply optional transformations to the data
- Persist the data through a configured Consumer
- Return generated keys or other operation results

Perfect for form submissions, data creation endpoints, and any operation that accepts and persists data.

### Data Sources (Bindings)

Bindings retrieve data from various sources for use by services. The framework includes the following binding types:

- **QueryBinding** - Executes SQL queries against databases via JNDI connections
- **FileSystemInputBinding** - Reads data from files on the server
- **ClasspathInputBinding** - Accesses resources from the application's classpath

### Parameters

Parameters extract values from HTTP requests. The framework includes:

- **RequestParameter** - Extracts a single value from request parameters with optional defaults
- **RequestArrayParameter** - Extracts multiple values as an array from a single parameter name
- **StaticParameter** - Provides fixed values that don't depend on the request

### Data Origins

Origins extract raw data from HTTP requests for processing. The framework provides:

- **RequestBodyOrigin** - Accesses the raw HTTP request body content
- **ParameterOrigin** - Extracts data from a specific request parameter

### Consumers

Consumers handle data persistence, used by InsertService to store processed data.

#### QueryConsumer

QueryConsumer persists JSON data to databases using SQL with features like:

- Parameterized SQL for secure database operations
- Automatic sequence ID generation
- Batch processing for multiple records

## JSP Custom Tags

Convirgance Web extends its functionality to the view layer with a comprehensive set of JSP custom tags that simplify data manipulation and presentation.

### Object and Array Tags

The framework provides tags for creating and manipulating JSON data structures directly in JSP pages:

- `<cv:object>` - Creates JSON objects with nested key-value pairs
- `<cv:array>` - Creates JSON arrays with nested values
- `<cv:key>` - Assigns values to named keys in objects
- `<cv:value>` - Adds values to arrays

### Data Access Tags

Several tags enable direct data access from JSP pages:

- `<cv:query>` - Executes database queries and stores results
- `<cv:service>` - Calls Convirgance Web services and captures results
- `<cv:set>` - Sets variables in different scopes

### Iteration and Control Tags

Tags for processing collections and controlling page flow:

- `<cv:iterate>` - Loops through collections with status information
- Conditional processing based on data properties

### Utility Functions

Expression Language functions enhance templates with data transformation capabilities:

- `cv:json()` - Converts objects to JSON strings
- `cv:html()` - Escapes text for safe HTML output
- `cv:urlparam()` - URL-encodes values for use in links
- Collection utilities like `first()` and `last()`

## Complete Example

Below is a complete example of a Spring XML configuration for a product service:

<!-- TODO SQL CDATA not behaving with docsify markdown renderer -->

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans ...>
    <!-- Customer Lookup Service (GET) -->
    <bean class="com.invirgance.convirgance.web.service.SelectService">
        <property name="parameters">
            <list>
                <bean class="com.invirgance.convirgance.web.parameter.RequestParameter">
                    <property name="name" value="zipcode" />
                    <property name="defaultValue" value="" />
                </bean>
                <bean class="com.invirgance.convirgance.web.parameter.RequestParameter">
                    <property name="name" value="state" />
                    <property name="defaultValue" value="" />
                </bean>
                <bean class="com.invirgance.convirgance.web.parameter.RequestParameter">
                    <property name="name" value="discountCode" />
                    <property name="defaultValue" value="" />
                </bean>
                <bean class="com.invirgance.convirgance.web.parameter.RequestParameter">
                    <property name="name" value="minPurchaseCount" />
                    <property name="defaultValue" value="0" />
                </bean>
                <bean class="com.invirgance.convirgance.web.parameter.RequestParameter">
                    <property name="name" value="nameSearch" />
                    <property name="defaultValue" value="" />
                </bean>
            </list>
        </property>
        <property name="binding">
            <bean class="com.invirgance.convirgance.web.binding.QueryBinding">
                <property name="jndiName" value="jdbc/CustomerDB"/>
                <property name="sql">
                    <value>
<![CDATA[
  SELECT * from APP.CUSTOMER
  WHERE (:zipcode = '' or ZIP = :zipcode)
  AND (:state = '' or STATE = :state)
  AND (:discountCode = '' or DISCOUNT_CODE = :discountCode)
]]>
                    </value>
                </property>
            </bean>
        </property>
        <property name="transformers">
            <list>
                <!-- Additional filtering based on purchase count -->
                <bean class="com.invirgance.convirgance.transform.filter.GreaterThanFilter">
                    <property name="key" value="PURCHASE_COUNT"/>
                    <property name="value">
                        <bean class="com.invirgance.convirgance.web.service.BindingParameter">
                            <property name="key" value="minPurchaseCount"/>
                        </bean>
                    </property>
                </bean>

                <!-- Text search on customer name -->
                <bean class="com.invirgance.convirgance.transform.filter.AndFilter">
                    <property name="filters">
                        <list>
                            <bean class="com.invirgance.convirgance.web.service.BindingFilter">
                                <property name="filter">
                                    <bean class="com.invirgance.convirgance.transform.filter.EqualsFilter">
                                        <property name="key" value="nameSearch"/>
                                        <property name="value" value=""/>
                                    </bean>
                                </property>
                            </bean>
                            <bean class="com.invirgance.convirgance.transform.filter.ContainsFilter">
                                <property name="key" value="NAME"/>
                                <property name="value">
                                    <bean class="com.invirgance.convirgance.web.service.BindingParameter">
                                        <property name="key" value="nameSearch"/>
                                    </bean>
                                </property>
                            </bean>
                        </list>
                    </property>
                </bean>
            </list>
        </property>
        <property name="output">
            <bean class="com.invirgance.convirgance.output.JSONOutput"/>
        </property>
    </bean>
</beans>
```

## Further Reading

<!-- TODO add public java doc link -->
<div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px">
  <span style="display: flex; align-items: center; justify-content: center;font-size:20px; width: 24px; height: 24px">📚</span>
  <a href="https://docs.invirgance.com/javadocs/convirgance/latest/com/invirgance/convirgance/dbms/package-summary.html">TODO JavaDocs: Convirgance (Web)</a>
</div>

## Sections

##### [Previous: JDBC](./convirgance-jdbc?id=convirgance-jdbc)

##### [Back to start?](./?id=convirgance)
