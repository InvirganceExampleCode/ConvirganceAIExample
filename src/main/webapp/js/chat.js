const toolActions = {};

async function processResponse(response, output)
{
    var reader = response.body.getReader();
    var complete = false;
    var text = "";
    var buffer = "";
    
    output.scrollTo(0, 0);

    while(!complete)
    {
        const { value, done } = await reader.read();
        const lines = new TextDecoder().decode(value).split(/\r?\n|\r/);

        for(var line of lines)
        {
            if(line.trim().length < 1) continue;

            try
            {
                line = JSON.parse(buffer + line);
                buffer = "";
            }
            catch(e)
            {
                console.log("Failed line:", line);
                console.error(e);
                
                buffer += line;
            }

            if(line.action)
            {
                if(toolActions[line.action]) toolActions[line.action](line);
                else console.error("Unable to handle action: " + line.action);
                
                continue;
            }

            text += line.message ? line.message.content : line.response;
            output.innerHTML = marked.parse(text);

            if(line.done) break;
        }

        complete = done;
    }
}

async function chat(url, question, output, callback)
{
    try
    {
        var response = await fetch(url + "?chat=" + encodeURIComponent(question));
        
        if(callback) callback(response.ok);

        await processResponse(response, output);
    }
    catch(e)
    {
        output.innerHTML = marked.parse("**I'm sorry!** I think I misunderstood and am unable to process your request. Can you rephrase your request?");

        console.error(e);
    }
}
