<%@page import="java.io.PrintWriter"%>
<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true"%>
<% if(exception != null) exception.printStackTrace(); %>

<html>
    <head>
        <title>An error has occurred</title>
    </head>
    <body>
        <h2>An error has occurred</h2>
        <p>
            <pre><%=exception.getMessage() %></pre>
        </p>
        <p>
            <pre><% exception.printStackTrace(new PrintWriter(out)); %></pre>
        </p>
    </body>
</html>