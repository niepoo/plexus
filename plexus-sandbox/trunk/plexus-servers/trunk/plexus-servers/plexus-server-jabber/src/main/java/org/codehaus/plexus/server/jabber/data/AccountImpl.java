/*
 * BSD License http://open-im.org/bsd-license.html
 * Copyright (c) 2003, OpenIM Project http://open-im.org
 * All rights reserved.
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the OpenIM project. For more
 * information on the OpenIM project, please see
 * http://open-im.org/
 */
package org.codehaus.plexus.server.jabber.data;


/**
 * @author AlAg
 * @avalon.component version="1.0" name="Account" lifestyle="transient"
 * @avalon.service type="org.codehaus.plexus.server.jabber.data.Account"
 */
public class AccountImpl
    implements java.io.Serializable, Account
{

    private String m_name;
    private String m_password;

    public final void setName( String name )
    {
        m_name = name;
    }

    public final String getName()
    {
        return m_name;
    }


    public final void setPassword( String password )
    {
        m_password = password;
    }

    public final String getPassword()
    {
        return m_password;
    }

    // -----------------------------------------------------------------------
    public boolean isAuthenticationTypeSupported( int type )
    {
        boolean isSupported = false;
        if ( type == Account.AUTH_TYPE_PLAIN )
        {
            isSupported = true;
        }
        return isSupported;
    }

    // -----------------------------------------------------------------------
    public final void authenticate( int type,
                                    String value,
                                    String sessionId )
        throws Exception
    {
        if ( type == AUTH_TYPE_PLAIN )
        {
            if ( !m_password.equals( value ) )
            {
                throw new Exception( "Unvalid plain password" );
            }
        }
        else if ( type == AUTH_TYPE_DIGEST )
        {
            throw new Exception( "Digest Authenticate not supported" );
        }
    }


    // -----------------------------------------------------------------------
    public String toString()
    {
        String s = "Username: " + m_name + " password: " + m_password;
        return s;
    }


}
