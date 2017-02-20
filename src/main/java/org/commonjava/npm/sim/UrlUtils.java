package org.commonjava.npm.sim;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public final class UrlUtils
{

    private UrlUtils()
    {
    }

    public static String buildUrl( final String baseUrl, final String basePath, final String[] parts )
            throws MalformedURLException
    {
        return buildUrl( baseUrl, basePath, null, parts );
    }

    public static String buildUrl( final String baseUrl, final String basePath, final Map<String, String> params,
                                   final String... parts )
            throws MalformedURLException
    {
        return new URL( buildPath( baseUrl, basePath, params, parts ) ).toExternalForm();
    }

    public static String buildPath( final String rootPath, final String baseSubPath, final Map<String, String> params,
                                    final String[] parts )
    {
        if ( parts == null || parts.length < 1 )
        {
            return rootPath;
        }

        final StringBuilder pathBuilder = new StringBuilder();

        if ( parts[0] == null || !parts[0].startsWith( rootPath ) )
        {
            pathBuilder.append( rootPath );
        }

        if ( baseSubPath != null && baseSubPath.length() > 0 )
        {
            appendPartTo( pathBuilder, baseSubPath );
        }

        for ( final String part : parts )
        {
            appendPartTo( pathBuilder, part );
        }

        if ( params != null && !params.isEmpty() )
        {
            pathBuilder.append( "?" );
            boolean first = true;
            for ( final Map.Entry<String, String> param : params.entrySet() )
            {
                if ( first )
                {
                    first = false;
                }
                else
                {
                    pathBuilder.append( "&" );
                }

                pathBuilder.append( param.getKey() )
                           .append( "=" )
                           .append( param.getValue() );
            }
        }

        return pathBuilder.toString();
    }

    private static void appendPartTo( final StringBuilder pathBuilder, String part )
    {
        if ( part == null || part.trim()
                                 .length() < 1 )
        {
            return;
        }

        if ( part.startsWith( "/" ) )
        {
            part = part.substring( 1 );
        }

        if ( pathBuilder.length() > 0 && pathBuilder.charAt( pathBuilder.length() - 1 ) != '/' )
        {
            pathBuilder.append( "/" );
        }

        pathBuilder.append( part );
    }

}