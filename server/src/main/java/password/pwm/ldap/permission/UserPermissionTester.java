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

package password.pwm.ldap.permission;

import password.pwm.PwmApplication;
import password.pwm.PwmConstants;
import password.pwm.bean.SessionLabel;
import password.pwm.bean.UserIdentity;
import password.pwm.config.value.data.UserPermission;
import password.pwm.error.PwmError;
import password.pwm.error.PwmOperationalException;
import password.pwm.error.PwmUnrecoverableException;
import password.pwm.http.CommonValues;
import password.pwm.ldap.search.SearchConfiguration;
import password.pwm.ldap.search.UserSearchEngine;
import password.pwm.util.java.TimeDuration;
import password.pwm.util.logging.PwmLogger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UserPermissionTester
{
    private static final PwmLogger LOGGER = PwmLogger.forClass( UserPermissionTester.class );

    public static boolean testUserPermission(
            final CommonValues commonValues,
            final UserIdentity userIdentity,
            final UserPermission userPermissions
    )
            throws PwmUnrecoverableException
    {
        return testUserPermission(
                commonValues.getPwmApplication(),
                commonValues.getSessionLabel(),
                userIdentity,
                Collections.singletonList( userPermissions ) );
    }

    public static boolean testUserPermission(
            final PwmApplication pwmApplication,
            final SessionLabel sessionLabel,
            final UserIdentity userIdentity,
            final List<UserPermission> userPermissions
    )
            throws PwmUnrecoverableException
    {
        if ( userPermissions == null )
        {
            return false;
        }

        final List<UserPermission> sortedList = new ArrayList<>( userPermissions );
        Collections.sort( sortedList );

        for ( final UserPermission userPermission : sortedList )
        {
            if ( testUserPermission( pwmApplication, sessionLabel, userIdentity, userPermission ) )
            {
                return true;
            }
        }
        return false;
    }

    private static boolean checkIfProfileAppliesToUser(
            final UserIdentity userIdentity,
            final UserPermission userPermission
    )
    {
        return userPermission.getLdapProfileID() == null
                || userPermission.getLdapProfileID().isEmpty()
                || userPermission.getLdapProfileID().equals( PwmConstants.PROFILE_ID_ALL )
                || userIdentity.getLdapProfileID().equals( userPermission.getLdapProfileID() );
    }

    private static boolean testUserPermission(
            final PwmApplication pwmApplication,
            final SessionLabel sessionLabel,
            final UserIdentity userIdentity,
            final UserPermission userPermission
    )
            throws PwmUnrecoverableException
    {
        if ( userPermission == null || userIdentity == null )
        {
            return false;
        }

        if ( !checkIfProfileAppliesToUser( userIdentity, userPermission ) )
        {
            return false;
        }

        final PermissionTypeHelper permissionTypeHelper = userPermission.getType().getPermissionTypeTester();
        final Instant startTime = Instant.now();
        final boolean match = permissionTypeHelper.testMatch( pwmApplication, sessionLabel, userIdentity, userPermission );
        LOGGER.debug( sessionLabel, () -> "user " + userIdentity.toDisplayString() + " is "
                + ( match ? "" : "not " )
                + "a match for permission '" + userPermission + "'",
                () -> TimeDuration.fromCurrent( startTime ) );
        return match;
    }

    public static List<UserIdentity> discoverMatchingUsers(
            final PwmApplication pwmApplication,
            final List<UserPermission> userPermissions,
            final SessionLabel sessionLabel,
            final int maxResultSize,
            final TimeDuration maxSearchTime
    )
            throws PwmUnrecoverableException, PwmOperationalException
    {
        if ( userPermissions == null )
        {
            return Collections.emptyList();
        }

        final List<UserPermission> sortedPermissions = new ArrayList<>( userPermissions );
        Collections.sort( sortedPermissions );

        final UserSearchEngine userSearchEngine = pwmApplication.getUserSearchEngine();
        final List<UserIdentity> resultSet = new ArrayList<>();

        for ( final UserPermission userPermission : sortedPermissions )
        {
            if ( ( maxResultSize ) - resultSet.size() > 0 )
            {
                final PermissionTypeHelper permissionTypeHelper = userPermission.getType().getPermissionTypeTester();
                final SearchConfiguration searchConfiguration = permissionTypeHelper.searchConfigurationFromPermission( userPermission )
                        .toBuilder()
                        .searchTimeout( maxSearchTime )
                        .build();

                try
                {
                    final Map<UserIdentity, Map<String, String>> results = userSearchEngine.performMultiUserSearch(
                            searchConfiguration,
                            ( maxResultSize ) - resultSet.size(),
                            Collections.emptyList(),
                            sessionLabel
                    );

                    resultSet.addAll( results.keySet() );
                }
                catch ( final PwmUnrecoverableException e )
                {
                    LOGGER.error( () -> "error reading matching users: " + e.getMessage() );
                    throw new PwmOperationalException( e.getErrorInformation() );
                }
            }
        }

        return Collections.unmodifiableList( resultSet );
    }

    static String profileIdForPermission( final UserPermission userPermission )
    {
        if ( userPermission.getLdapProfileID() != null
                && !userPermission.getLdapProfileID().isEmpty()
                && !userPermission.getLdapProfileID().equals( PwmConstants.PROFILE_ID_ALL ) )
        {
            return userPermission.getLdapProfileID();
        }

        return null;
    }


    public static void validatePermissionSyntax( final UserPermission userPermission )
            throws PwmUnrecoverableException
    {
        Objects.requireNonNull( userPermission );

        if ( userPermission.getType() == null )
        {
            throw PwmUnrecoverableException.newException( PwmError.CONFIG_FORMAT_ERROR, "userPermission must have a type value" );
        }

        final PermissionTypeHelper permissionTypeHelper = userPermission.getType().getPermissionTypeTester();
        permissionTypeHelper.validatePermission( userPermission );
    }
}