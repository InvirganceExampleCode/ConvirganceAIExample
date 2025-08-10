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
import static com.invirgance.example.todo.TodoList.Status;
import java.util.Comparator;

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
    
    private static boolean isNumber(String value)
    {
        if(value.length() < 1) return false;
        
        for(int i=0; i<value.length(); i++)
        {
            if(!Character.isDigit(value.charAt(i))) return false;
        }
        
        return true;
    }
    
    private static String transformDate(long date)
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
        
        if(text.length() < 1) text.append("Just now");
        else text.append(" ago");
        
        return text.toString();
    }
    
    private Iterable<JSONObject> getTasks()
    {
        return new IdentityTransformer() {
            @Override
            public JSONObject transform(JSONObject record) throws ConvirganceException
            {
                if(record.getLong("created") == record.getLong("updated")) record.put("updated", "Not updated");
                else record.put("updated", transformDate(record.getLong("updated")));
                
                record.put("created", transformDate(record.getLong("created")));
                
                return record;
            }
        }.transform(TodoList.list());
    }
    
    @Tool("Get a count of all tasks regardless of state")
    public int countAllTasks()
    {
        var count = 0;
        
        for(var item : getTasks()) count++;
        
        return count;
    }
    
    @Tool("Get a count of tasks for a state")
    public int count(
            @ToolParam("State to filter the task list by") Status state)
    {
        var count = 0;
        var value = state.toString();
        
        for(var item : getTasks()) 
        {
            if(item.getString("state").equals(value))
            {
                count++;
            }
        }
        
        return count;
    }
    
    @Tool("Returns the complete list of tasks")
    public String listTasks()
    {
        var list = new JSONArray<JSONObject>();
        
        for(var item : getTasks()) 
        {   
            list.add(item);
        }
        
        list = new JSONArray<>(list.reversed());
        
        return table(list, true);
    }
    
    @Tool("Obtains a list of all tasks in the requested state")
    public String listTasksByState(
            @ToolParam("The state of the desired tasks") Status state)
    {
        var results = new JSONArray<JSONObject>();
        
        for(var record : getTasks())
        {
            if(record.getString("state").equals(state.toString()))
            {
                results.add(record);
            }
        }
        
        return table(results, true);
    }
    
    @Tool("Returns the newest task")
    public String newest()
    {
        return newestList(1);
    }
    
    @Tool("Returns the most recently created tasks in order of creation")
    public String newestList(
            @ToolParam("Number of tasks to return. Pass 1 if you only need to know the newest.") int count)
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

        return table(results, true);
    }
    
    @Tool("Returns the oldest task")
    public String oldest()
    {
        return oldestList(1);
    }
    
    @Tool("Returns the oldest tasks in reverse order of creation")
    public String oldestList(
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

        return table(results, true);
    }
    
    @Tool("Returns the most recently updated task")
    public String mostRecentlyUpdated()
    {
        return mostRecentlyUpdatedList(1);
    }
    
    @Tool("Returns the most recently updated tasks in order of creation")
    public String mostRecentlyUpdatedList(
            @ToolParam("Number of tasks to return. Pass 1 if you only need to know the newest.") int count)
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

        return table(results, true);
    }
    
    @Tool("Creates a new todo task. This is an irreversible task, so only call when you intend to create a task. Returns the details of the created task.")
    public String createTask(
            @ToolParam("Description or title of the task") String task)
    {
        long id = TodoList.insert(task);
        
        return getTask(id);
    }
    
    @Tool("Get a task by its numeric identifier")
    public String getTask(
            @ToolParam("Numeric identifier for the task") long id)
    {
        JSONObject record = TodoList.get(id);
        
        return table(new JSONArray<JSONObject>(record), false);
    }
    
    @Tool("Updates the state of a task. Returns the details of the updated task.")
    public String updateTask(
            @ToolParam("Numeric identifier for the task to update") long id,
            @ToolParam("The new state for the task") Status state)
    {
        var result = TodoList.update(id, state);
        
        if(result == null) return "Unable to update task with id " + id;
        
        result.put("created", transformDate(result.getLong("created")));
        result.put("updated", transformDate(result.getLong("updated")));
        
        return result.toString();
    }
    
    @Tool("Updates the state of the most recently created task. Returns the details of the updated task.")
    public String updateMostRecentTask(
            @ToolParam("The new state for the task") Status state)
    {
        JSONObject recent = null;
        
        for(var record : TodoList.list())
        {
            if(recent == null || record.getLong("created") > recent.getLong("created"))
            {
                recent = record;
            }
        }
        
        if(recent == null) return "No task was found to update";
        
        recent = TodoList.update(recent.getLong("id"), state);
        
        return table(new JSONArray<>(recent), true);
    }
    
    @Tool("Changes the state of the task that was last updated. Perfect for reversing a change. Returns the details of the updated task.")
    public String updateMostRecentlyUpdatedTask(
            @ToolParam("The new state for the task") Status state)
    {
        JSONObject recent = null;
        
        for(var record : TodoList.list())
        {
            if(recent == null || record.getLong("updated") > recent.getLong("updated"))
            {
                recent = record;
            }
        }
        
        if(recent == null) return "No task was found to update";
        
        recent = TodoList.update(recent.getLong("id"), state);
        
        return table(new JSONArray<>(recent), true);
    }
    
    public static String table(Iterable<JSONObject> todos, boolean extended)
    {
        StringBuffer buffer = new StringBuffer();
        int count = 0;
        
        buffer.append("\n");
        buffer.append("| ID | Task | State |");
        
        if(extended) buffer.append(" | Created | Updated |\n");
        else buffer.append("\n");
        
        buffer.append("|----|------|-------|");
        
        if(extended) buffer.append(" |---------|---------|\n");
        else buffer.append("\n");
        
        for(var record : todos)
        {
            buffer.append("| ").append(record.getString("id"));
            buffer.append(" | ").append(record.getString("text"));
            buffer.append(" | ").append(record.getString("state"));
            
            if(extended)
            {
                if(isNumber(record.getString("created")))
                {
                    if(record.getLong("created") == record.getLong("updated")) record.put("updated", "Not updated");
                    else record.put("updated", transformDate(record.getLong("updated")));

                    record.put("created", transformDate(record.getLong("created")));
                }
                
                buffer.append(" | ").append(record.getString("created"));
                buffer.append(" | ").append(record.getString("updated"));
            }
            
            buffer.append(" |\n");
            
            count++;
        }
        
        if(count < 1) buffer.append("\nThere are no tasks in the todo list.\n");
        
        return buffer.toString();
    }
}
