package org.codehaus.plexus.test;

public class DefaultComponent
    implements Component
{
    private String host;

    private int port;

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }
}
