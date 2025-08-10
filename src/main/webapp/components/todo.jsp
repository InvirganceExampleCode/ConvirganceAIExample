<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="convirgance:web" prefix="virge" %>

<virge:service var="todos" path="/services/todo" />

<table>
    <thead>
        <tr>
            <th>#</th>
            <th>Todo Item</th>
            <th>Current Status</th>
            <th>Created</th>
            <th>Updated</th>
        </tr>
    </thead>
    <tbody>
    <virge:iterate var="todo" items="${todos}">
        <tr>
            <td>${todo.id}</td>
            <td>${todo.text}</td>
            <td>${todo.state}</td>
            <td class="timestamp">${todo.created}</td>
            <td class="timestamp">${todo.updated}</td>
        </tr>
    </virge:iterate>
    </table>
</table>