/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.changerequest.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.AbstractResourceReferenceResolver;

/**
 * Default resolver to resolve {@link ExtendedURL} to {@link ChangeRequestReference}.
 * The URL format handled is {@code https://server/context/changerequest/wiki/action/identifier} where {@code wiki}
 * is the name of the wiki where the action should take place, the {@code action} is a
 * {@link org.xwiki.contrib.changerequest.ChangeRequestReference.ChangeRequestAction} and {@code identifier} is
 * optional for create and for other actions is the identifier of the change request.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Named("changerequest")
@Singleton
public class ChangeRequestReferenceResolver extends AbstractResourceReferenceResolver
{
    private static final String DEFAULT_LOCALE = "UTF-8";

    @Override
    public ResourceReference resolve(ExtendedURL extendedURL, ResourceType resourceType,
        Map<String, Object> parameters) throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        CreateResourceReferenceException urlFormatException = new CreateResourceReferenceException(
            String.format("Invalid Change Request URL format: the format should contain a wiki name, an action and an "
                + "ID. Provided URL: [%s]", extendedURL));
        List<String> segments = extendedURL.getSegments();
        if (segments.isEmpty() || segments.size() > 3) {
            throw urlFormatException;
        } else {
            String wikiName = segments.get(0);
            String actionString = segments.get(1);

            ChangeRequestReference.ChangeRequestAction changeRequestAction = getChangeRequestAction(actionString);

            // It's only possible to omit the identifier in case of create action
            if (changeRequestAction != ChangeRequestReference.ChangeRequestAction.CREATE && segments.size() == 2) {
                throw urlFormatException;
            }

            String changeRequestId = null;
            if (segments.size() == 3) {
                String identifier = segments.get(2);
                try {
                    changeRequestId = URLDecoder.decode(identifier, DEFAULT_LOCALE);
                } catch (UnsupportedEncodingException e) {
                    throw new UnsupportedResourceReferenceException(
                        String.format("Error while decoding Change Request ID [%s]: [%s].",
                            identifier,
                            ExceptionUtils.getRootCauseMessage(e)));
                }
            }
            ChangeRequestReference result =
                new ChangeRequestReference(new WikiReference(wikiName), changeRequestAction, changeRequestId);
            copyParameters(extendedURL, result);
            return result;
        }
    }

    private ChangeRequestReference.ChangeRequestAction getChangeRequestAction(String actionString)
        throws UnsupportedResourceReferenceException
    {
        try {
            return ChangeRequestReference.ChangeRequestAction.valueOf(actionString.toUpperCase());
        } catch (IllegalArgumentException e)
        {
            throw new UnsupportedResourceReferenceException(
                String.format("The given action is invalid for Change Request: [%s].", actionString));
        }
    }
}
