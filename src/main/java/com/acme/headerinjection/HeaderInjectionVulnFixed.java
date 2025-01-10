package com.acme.headerinjection;

import io.github.pixee.security.Newlines;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import javax.servlet.http.HttpServletResponse;

@Path("/unsafe-header-injection")
public class HeaderInjectionVulnFixed {

    @GET
    public String lookupResource(HttpServletResponse response, @QueryParam("q") final String q) {
        response.setHeader("X-Last-Search", Newlines.stripAll(stripNewlines(q)));
        return "ok";
    }

    private static String stripNewlines(final String q) {
        return q.replaceAll("[\n\r]", "");
    }
}
