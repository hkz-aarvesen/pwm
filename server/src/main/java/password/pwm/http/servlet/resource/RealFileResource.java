/*
 * Password Management Servlets (PWM)
 * http://www.pwm-project.org
 *
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2020 The PWM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package password.pwm.http.servlet.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class RealFileResource implements FileResource
{
    private final File realFile;

    RealFileResource( final File realFile )
    {
        this.realFile = realFile;
    }

    public InputStream getInputStream( ) throws IOException
    {
        return new FileInputStream( realFile );
    }

    public long length( )
    {
        return realFile.length();
    }

    public long lastModified( )
    {
        return realFile.lastModified();
    }

    public boolean exists( )
    {
        return realFile.exists();
    }

    public String getName( )
    {
        return realFile.getAbsolutePath();
    }
}
