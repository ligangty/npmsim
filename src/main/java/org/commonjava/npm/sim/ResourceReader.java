package org.commonjava.npm.sim;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class ResourceReader
{
    private static final Logger logger = LoggerFactory.getLogger( ResourceReader.class );

    public static String getJson( String resource )
    {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( resource ))
        {
            if ( stream == null )
            {
                logger.error( "Cannot find classpath resource: {}", resource );
                return "";
            }

            return IOUtils.toString( stream );
        }
        catch ( IOException e )
        {
            logger.error( "Cannot read resource: {}", resource );
            e.printStackTrace();
            return "";
        }
    }

    public static InputStream getPkg( String resource )
    {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( resource );
        if ( stream == null )
        {
            logger.error( "Cannot find classpath resource pkg: {} ", resource );
            return null;
        }

        return stream;
    }
}
