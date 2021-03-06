package org.codehaus.plexus.util.cli.shell;

/*
 * Copyright 2007 The Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jason van Zyl
 * @version $Id$
 */
public class BourneShell
    extends Shell
{
    private static final char[] BASH_QUOTING_TRIGGER_CHARS = {
        ' ',
        '$',
        ';',
        '&',
        '|',
        '<',
        '>',
        '*',
        '?',
        '(',
        ')',
        '[',
        ']',
        '{',
        '}',
        '`' };

    public BourneShell()
    {
        this( false );
    }

    public BourneShell( boolean isLoginShell )
    {
        setShellCommand( "/bin/sh" );
        setArgumentQuoteDelimiter( '\'' );
        setExecutableQuoteDelimiter( '\"' );
        setSingleQuotedArgumentEscaped( true );
        setSingleQuotedExecutableEscaped( false );
        setQuotedExecutableEnabled( true );

        if ( isLoginShell )
        {
            addShellArg( "-l" );
        }
    }

    /** {@inheritDoc} */
    public String getExecutable()
    {
        if ( Os.isFamily( "windows" ) )
        {
            return super.getExecutable();
        }

        if ( ( super.getExecutable() != null ) && ( super.getExecutable().indexOf( " " ) == -1 )
            && ( super.getExecutable().indexOf( "'" ) != -1 ) )
        {
            return StringUtils.replace( super.getExecutable(), "'", "\\\'" );
        }

        return super.getExecutable();
    }

    public List getShellArgsList()
    {
        List shellArgs = new ArrayList();
        List existingShellArgs = super.getShellArgsList();

        if ( ( existingShellArgs != null ) && !existingShellArgs.isEmpty() )
        {
            shellArgs.addAll( existingShellArgs );
        }

        shellArgs.add( "-c" );

        return shellArgs;
    }

    public String[] getShellArgs()
    {
        String[] shellArgs = super.getShellArgs();
        if ( shellArgs == null )
        {
            shellArgs = new String[0];
        }

        if ( ( shellArgs.length > 0 ) && !shellArgs[shellArgs.length - 1].equals( "-c" ) )
        {
            String[] newArgs = new String[shellArgs.length + 1];

            System.arraycopy( shellArgs, 0, newArgs, 0, shellArgs.length );
            newArgs[shellArgs.length] = "-c";

            shellArgs = newArgs;
        }

        return shellArgs;
    }

    protected String getExecutionPreamble()
    {
        if ( getWorkingDirectoryAsString() == null )
        {
            return null;
        }

        String dir = getWorkingDirectoryAsString();
        StringBuffer sb = new StringBuffer();
        sb.append( "cd " );
        if ( dir != null && dir.indexOf( " " ) == -1 && dir.indexOf( "'" ) != -1 )
        {
            dir = StringUtils.replace( dir, "'", "\\\'" );
        }

        sb.append( StringUtils.quoteAndEscape( dir, '\"' ) );
        sb.append( " && " );

        return sb.toString();
    }

    protected char[] getQuotingTriggerChars()
    {
        return BASH_QUOTING_TRIGGER_CHARS;
    }
}
