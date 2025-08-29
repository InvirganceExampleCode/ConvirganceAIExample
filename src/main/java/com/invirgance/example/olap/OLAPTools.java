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
package com.invirgance.example.olap;

import com.invirgance.convirgance.ConvirganceException;
import com.invirgance.convirgance.ai.annotations.Tool;
import com.invirgance.convirgance.ai.annotations.ToolParam;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.olap.Star;
import com.invirgance.convirgance.source.FileSource;
import com.invirgance.convirgance.web.http.HttpRequest;
import com.invirgance.convirgance.web.servlet.ServiceState;
import com.invirgance.convirgance.wiring.XMLWiringParser;
import com.invirgance.convirgance.wiring.annotation.Wiring;
import java.io.File;
import java.util.List;

/**
 *
 * @author jbanes
 */
@Wiring
public class OLAPTools
{
    private String schema;
    private File file;
    private Star star;
    
    private long loaded;

    
    private Star loadStar()
    {
        List list;

        // Already loaded
        if(star != null && this.file.lastModified() <= this.loaded) return star;
        
        synchronized(this)
        {
            file = ((HttpRequest)ServiceState.get("request")).getFileByPath("WEB-INF/models/" + schema);

            if(file == null) throw new ConvirganceException("Schema " + schema + " not found under WEB-INF/models/");

            list = new XMLWiringParser<List>(new FileSource(file)).getRoot();
            loaded = file.lastModified();

            for(Object object : list)
            {
                if(object instanceof Star) this.star = (Star)object;
            }
        }
        
        return star;
    }
    
    public String getStar()
    {
        return schema;
    }

    public void setStar(String path)
    {
        this.schema = path;
        this.file = ((HttpRequest)ServiceState.get("request")).getFileByPath(path);
        
        loadStar();
    }
    
    public String getDimensionsAndMeasures()
    {
        var star = loadStar();
        var buffer = new StringBuffer();
        
        buffer.append("| Type      | Name           |\n");
        buffer.append("|-----------|----------------|\n");
        
        for(var dimension : star.getDimensions())
        {
            buffer.append("| dimension | ").append(dimension.getName()).append(" |\n");
        }
        
        for(var measure : star.getMeasures())
        {
            buffer.append("| measure   | ").append(measure.getName()).append(" |\n");
        }
        
        return buffer.toString();
    }
    
    @Tool("Provides a list of OLAP Dimensions contained in the Star Schema")
    public String getDimensions()
    {
        StringBuffer buffer = new StringBuffer();
        JSONArray values = new JSONArray();
        
        for(var dimension : loadStar().getDimensions())
        {
            if(buffer.length() > 0) buffer.append(", ");
            
            buffer.append(dimension.getName());
            values.add(dimension.getName());
        }
        
        return buffer.toString();
//        return values.toString();
    }
    
    @Tool("Provides a list of OLAP Measures contained in the Star Schema")
    public String getMeasures()
    {
        StringBuffer buffer = new StringBuffer();
        JSONArray values = new JSONArray();
        
        for(var measure : loadStar().getMeasures())
        {
            if(buffer.length() > 0) buffer.append(", ");
            
            buffer.append(measure.getName());
            values.add(measure.getName());
        }
        
        return buffer.toString();
//        return values.toString();
    }
    
    @Tool("Call this when the user is asking for suggestions")
    public String handleSuggestions(
            @ToolParam("Details of the user's request") String request)
    {
        return "Analyze the list of dimensions and measures to suggest possible reports the user could run";
    }
    
    @Tool("Call this when the user wants an OLAP report generated.")
    public String generateReport(
            @ToolParam("List of dimensions to include in the report. Send list of names as a JSON array.") String dimensions, 
            @ToolParam("List of measures to include in the report. Send list of names as a JSON array.") String measures)
    {
        JSONObject request = new JSONObject();
        String dimension;
        String measure;
        
        JSONArray parsedDimensions;
        JSONArray parsedMeasures;
        
        try
        {
            if(dimensions == null) dimensions = "[]";
            if(measures == null) measures = "[]";
            
            dimensions = dimensions.trim();
            measures = measures.trim();
            
            // Handle empty strings
            if(dimensions.isEmpty() && measures.isEmpty()) return "";
            if(dimensions.length() < 1) dimensions = "[]";
            if(measures.length() < 1) measures = "[]";
            
            // Handle single quotes instead of double quotes
            if(!dimensions.contains("\"") && dimensions.contains("'")) dimensions = dimensions.replace("'", "\"");
            if(!measures.contains("\"") && measures.contains("'")) measures = measures.replace("'", "\"");
            
            parsedDimensions = new JSONArray(dimensions);
            parsedMeasures = new JSONArray(measures);
        }
        catch(Exception e)
        {
            System.out.println("Dimensions: " + dimensions);
            System.out.println("Measures: " + measures);
            
            e.printStackTrace();
            
            return "Your inputs were not valid JSON. Please review the dimensions and measures list to ensure that you correctly passed JSON and try again.";
        }
        
        for(int i=0; i<parsedDimensions.size(); i++)
        {
            dimension = parsedDimensions.getString(i);
            
            if(star.getDimension(dimension) == null)
            {
                System.err.println("Invalid dimension: " + dimension);
                System.err.println("Dimensions: " + dimensions);
                System.err.println("Measures: " + measures);
                
                if(star.getMeasure(dimension) != null)
                {
                    System.err.println("Measure " + dimension + " accidentally passed as dimension.");
                    
                    parsedMeasures.add(0, dimension);
                    parsedDimensions.remove(i--);
                    
                    continue;
                }
                
                return "You passed an invalid dimension name. Double check the name and try again. Invalid dimension was: " + dimension;
            }
        }
        
        try
        {
            request.put("dimensions", parsedDimensions);
            request.put("measures", parsedMeasures);
            
            ServiceState.set("report", request);
        }
        catch(Exception e)
        {
            System.err.println("Dimensions: " + dimensions);
            System.err.println("Measures: " + measures);
            
            e.printStackTrace();
            
            return "Explain to the user that there was an error generating the report and provide helpful suggestions for how to correct the error. Do not respond with data. Do not respond with a report. Here is the error that occurred when attempting to generate the report: " + e.getMessage();
        }
        
        return "Report successfully generated. The user will see the report on their screen. Instruct the user to view the report output. Do not attempt to answer the question further.";
    }
}
