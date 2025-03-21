package com.acme.jndi;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.regex.Pattern;

@Path("/unsafe-jndi-lookup")
public class JNDIVulnFixed3 {

    @GET
    public String lookupResource(@QueryParam("resource") final String resource) throws NamingException {
        Context ctx = new InitialContext();

        if(Pattern.compile("^ldap://$").matcher(resource).matches()) {
            throw new SecurityException("Illegal JNDI resource name: " + resource);
        }
        Object obj = ctx.lookup(resource);
        return String.valueOf(obj);
    }
}
