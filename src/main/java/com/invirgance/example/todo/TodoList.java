/*
 * The MIT License
 *
 * Copyright 2025 INVIRGANCE LLC.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.invirgance.example.todo;

import com.invirgance.convirgance.ConvirganceException;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.transform.IdentityTransformer;
import static com.invirgance.example.todo.TodoList.Status.*;

/**
 *
 * @author jbanes
 */
public class TodoList
{
    private static final JSONArray<JSONObject> todos = new JSONArray<>();
    private static long index = 1;
    
    public static long insert(String text)
    {
        var todo = new JSONObject();
        long time = System.currentTimeMillis();
        var id = 0L;
        
        synchronized(todos)
        {
            id = index++;
            
            todo.put("id", id);
            todo.put("text", text);
            todo.put("state", TODO.toString());
            todo.put("created", time);
            todo.put("updated", time);
            
            todos.add(todo);
        }
        
        return id;
    }
    
    public static Iterable<JSONObject> list()
    {
        return new IdentityTransformer() {
            @Override
            public JSONObject transform(JSONObject record) throws ConvirganceException
            {
                return new JSONObject(record);
            }
        }.transform(todos);
    }
    
    public static enum Status
    {
        TODO,
        DOING,
        DONE,
        CANCEL,
        BLOCKED
    }
}
