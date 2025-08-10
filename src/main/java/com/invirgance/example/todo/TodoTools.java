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
import com.invirgance.convirgance.ai.annotations.Tool;
import com.invirgance.convirgance.ai.annotations.ToolParam;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.transform.IdentityTransformer;
import com.invirgance.convirgance.wiring.annotation.Wiring;
import static com.invirgance.example.todo.TodoList.Status.*;
import java.util.Comparator;
import java.util.Date;

/**
 *
 * @author jbanes
 */
@Wiring
public class TodoTools
{
    private static final long SECOND = 1000;
    private static final long MINUTE = SECOND * 60;
    private static final long HOUR = MINUTE * 60;
    private static final long DAY = HOUR * 24;
    private static final long YEAR = DAY * 365;
    
    private String transformDate(long date)
    {
        StringBuffer text = new StringBuffer();
        JSONArray<String> components = new JSONArray<>();
        
        long delta = System.currentTimeMillis() - date;
        long years = 0;
        long days = 0;
        long hours = 0;
        long minutes = 0;
        
        while(delta > YEAR) { years++; delta -= YEAR; }
        while(delta > DAY) { days++; delta -= DAY; }
        while(delta > HOUR) { hours++; delta -= HOUR; }
        while(delta > MINUTE) { minutes++; delta -= MINUTE; }
        
        if(years > 0)
        {
            text.append(years).append(" ").append((years > 1) ? "years" : "year");
            components.add(text.toString());
            text.setLength(0);
        }
        
        if(days > 0)
        {
            text.append(days).append(" ").append((days > 1) ? "days" : "day");
            components.add(text.toString());
            text.setLength(0);
        }
        
        if(hours > 0)
        {
            text.append(hours).append(" ").append((hours > 1) ? "hours" : "hour");
            components.add(text.toString());
            text.setLength(0);
        }
        
        if(minutes > 0)
        {
            text.append(minutes).append(" ").append((minutes > 1) ? "minutes" : "minute");
            components.add(text.toString());
            text.setLength(0);
        }
        
        for(String component : components)
        {
            if(text.length() > 0) text.append(", ");
            
            text.append(component);
        }
        
        if(components.size() < 2 && delta > SECOND)
        {
            if(text.length() > 0) text.append(", ");
            
            text.append((delta / SECOND)).append(" ").append(((delta / SECOND) > 1) ? "second" : "seconds");
        }
            
        text.append(" ago");
        
        return text.toString();
    }
    
    private Iterable<JSONObject> getTasks()
    {
        return new IdentityTransformer() {
            @Override
            public JSONObject transform(JSONObject record) throws ConvirganceException
            {
                record.put("created", transformDate(record.getLong("created")));
                record.put("updated", transformDate(record.getLong("updated")));
                
                return record;
            }
        }.transform(TodoList.list());
    }
    
    @Tool("Get a count of tasks in todo status")
    public int countTodo()
    {
        var count = 0;
        
        for(var item : getTasks()) 
        {
            if(item.getString("state").equals(TODO.toString()))
            {
                count++;
            }
        }
        
        return count;
    }
    
    @Tool("Returns a list of up to 25 tasks in todo status")
    public String listTodo()
    {
        var list = new JSONArray<JSONObject>();
        
        for(var item : getTasks())
        {
            if(item.getString("state").equals(TODO.toString()))
            {
                list.add(item);
            }
            
            if(list.size() >= 25) break;
        }
        
        list = new JSONArray<>(list.reversed());
        
        return list.toString(4);
    }
    
    @Tool("Returns the newest task or tasks in order of creation")
    public String newest(
            @ToolParam("Number of tasks to return. Pass 1 if you only need to know the latest.") int count)
    {
        var list = new JSONArray<JSONObject>();
        var results = new JSONArray<JSONObject>();
        
        for(var record : TodoList.list()) list.add(record);
        
        list.sort(new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject left, JSONObject right)
            {
                return (int)(right.getLong("created") - left.getLong("created"));
            }
        });
        
        for(var item : list)
        {
            if(item.getLong("created") == item.getLong("updated")) item.put("updated", "Not updated");
            else item.put("updated", transformDate(item.getLong("updated")));
            
            item.put("created", transformDate(item.getLong("created")));
            results.add(item);
            
            if(results.size() >= count) break;
        }

        return results.toString(4);
    }
    
    @Tool("Returns the oldest task or tasks in order of creation")
    public String oldest(
            @ToolParam("Number of tasks to return. Pass 1 if you only need to know the oldest.") int count)
    {
        var list = new JSONArray<JSONObject>();
        var results = new JSONArray<JSONObject>();
        
        for(var record : TodoList.list()) list.add(record);
        
        list.sort(new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject left, JSONObject right)
            {
                return (int)(left.getLong("created") - right.getLong("created"));
            }
        });
        
        for(var item : list)
        {
            if(item.getLong("created") == item.getLong("updated")) item.put("updated", "Not updated");
            else item.put("updated", transformDate(item.getLong("updated")));
            
            item.put("created", transformDate(item.getLong("created")));
            results.add(item);
            
            if(results.size() >= count) break;
        }

        return results.toString(4);
    }
}
