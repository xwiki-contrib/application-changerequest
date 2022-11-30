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
package org.xwiki.contrib.changerequest.internal.ui;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.template.TemplateManager;
import org.xwiki.uiextension.UIExtension;

/**
 * UI Extension to display the change request user profile tab.
 * Note that this UIX existed before {@code 1.4} as an XWiki page.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named(ChangeRequestUserProfileUIExtension.ID)
@Singleton
public class ChangeRequestUserProfileUIExtension implements UIExtension
{
    /**
     * UIX ID.
     */
    public static final String ID = "org.xwiki.contrib.changerequest.user.profile.menu";

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private TemplateManager templates;

    private Map<String, String> parameters;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.plaftorm.user.profile.menu";
    }

    @Override
    public Map<String, String> getParameters()
    {
        if (this.parameters == null) {
            parameters = new HashMap<>();
            parameters.put("icon", "branch");
            parameters.put("name", this.localizationManager.getTranslationPlain("changerequest.user.profile.menu"));
            parameters.put("priority", "1100");
        }
        return this.parameters;
    }

    @Override
    public Block execute()
    {
        return this.templates.executeNoException("changerequest/userprofile.vm");
    }
}
