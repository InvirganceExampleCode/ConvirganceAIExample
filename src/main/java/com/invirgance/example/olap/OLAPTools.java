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
    
    @Tool("Provides a list of OLAP Dimensions contained in the Star Schema")
    public String getDimensions()
    {
        StringBuffer buffer = new StringBuffer();
        
        for(var dimension : loadStar().getDimensions())
        {
            if(buffer.length() > 0) buffer.append(", ");
            
            buffer.append(dimension.getName());
        }
        
        return buffer.toString();
    }
    
    @Tool("Provides a list of OLAP Measures contained in the Star Schema")
    public String getMeasures()
    {
        StringBuffer buffer = new StringBuffer();
        
        for(var measure : loadStar().getMeasures())
        {
            if(buffer.length() > 0) buffer.append(", ");
            
            buffer.append(measure.getName());
        }
        
        return buffer.toString();
    }
    
    @Tool("Generate an OLAP report using the specified dimensions and measures. Call when the user requests a report to be generated.")
    public String generateReport(
            @ToolParam("List of dimensions to include in the report. Only dimensions from the available dimensions list can be passed.") String dimensions, 
            @ToolParam("List of measures to include in the report. Only measures from the available measures list can be passed.") String measures)
    {
        try
        {
            var request = new JSONObject();
            
            dimensions = dimensions.trim();
            measures = measures.trim();
            
            if(dimensions.isEmpty() && measures.isEmpty()) return "";
            
            // Handle empty strings
            if(dimensions.length() < 1) dimensions = "[]";
            if(measures.length() < 1) measures = "[]";
            
            // Handle single quotes instead of double quotes
            if(!dimensions.contains("\"") && dimensions.contains("'")) dimensions = dimensions.replace("'", "\"");
            if(!measures.contains("\"") && measures.contains("'")) measures = measures.replace("'", "\"");
            
            request.put("dimensions", new JSONArray(dimensions));
            request.put("measures", new JSONArray(measures));
            
            ServiceState.set("report", request);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            
            return "The following error occurred when attempting to generate the report: " + e.getMessage();
        }
        
        return "Report successfully generated. The user will see the report on their screen. Instruct the user to view the report output. Do not attempt to answer the question further.";
    }
}
