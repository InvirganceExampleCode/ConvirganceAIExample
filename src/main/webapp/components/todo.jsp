<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="convirgance:web" prefix="virge" %>

<virge:service var="todos" path="/services/todo" />

<table>
    <thead>
        <tr>
            <th>#</th>
            <th>Todo Item</th>
            <th>Status</th>
            <th class="hide-mobile">Created</th>
            <th class="hide-mobile">Updated</th>
        </tr>
    </thead>
    <tbody>
    <virge:iterate var="todo" items="${todos}">
        <tr>
            <td>${todo.id}</td>
            <td>${todo.text}</td>
            <td>${todo.state}</td>
            <td class="timestamp hide-mobile">${todo.created}</td>
            <td class="timestamp hide-mobile">${todo.updated}</td>
        </tr>
    </virge:iterate>
    </table>
</table>