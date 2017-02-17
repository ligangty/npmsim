package org.commonjava.npm.sim;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by gli on 17-2-17.
 */
public interface ServiceHandler
{
    void handle( final HttpServletRequest req, final HttpServletResponse resp )
            throws ServletException, IOException;
}
