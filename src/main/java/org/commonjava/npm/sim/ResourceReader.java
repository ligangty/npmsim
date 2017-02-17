/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.npm.sim;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by gli on 2/17/17.
 */
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
