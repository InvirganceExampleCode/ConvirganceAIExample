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

import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.transform.Transformer;
import com.invirgance.convirgance.web.servlet.ServiceState;
import com.invirgance.convirgance.wiring.annotation.Wiring;
import java.util.Iterator;

/**
 *
 * @author jbanes
 */
@Wiring
public class OLAPReportTransformer implements Transformer
{
    @Override
    public Iterator<JSONObject> transform(Iterator<JSONObject> iterator)
    {
        return new Iterator<JSONObject>() {
            @Override
            public boolean hasNext()
            {
                if(ServiceState.get("report") != null) return true;
                
                return iterator.hasNext();
            }

            @Override
            public JSONObject next()
            {
                var report = (JSONObject)ServiceState.get("report");
                
                if(report != null)
                {
                    report.put("action", "generateReport");
                    ServiceState.set("report", null);
                    
                    return report;
                }
                
                return iterator.next();
            }
        };
    }
    
}
