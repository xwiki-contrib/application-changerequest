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
package org.xwiki.contrib.changerequest.internal.diff;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.skinx.SkinExtensionPluginApi;

/**
 * Helper for finding the required skin extensions when performing the diff of a page.
 * Note that this component is a copy/paste of the same component available in xwiki-platform-export-pdf-api but
 * which was only internally exposed. This instance should be replaced once
 * <a href="https://jira.xwiki.org/browse/XWIKI-23878">XWIKI-23878</a> is done.
 *
 * @version $Id$
 * @since 1.21.1
 */
@Component(roles = RequiredSkinExtensionsRecorder.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class RequiredSkinExtensionsRecorder
{
    private static final List<String> SKIN_EXTENSION_PLUGINS =
        Arrays.asList("ssrx", "ssfx", "ssx", "linkx", "jsrx", "jsfx", "jsx");

    private final Map<String, String> requiredSkinExtensionsMap = new LinkedHashMap<>();

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Start recording.
     */
    public void start()
    {
        this.requiredSkinExtensionsMap.clear();
        for (String pluginName : SKIN_EXTENSION_PLUGINS) {
            this.requiredSkinExtensionsMap.put(pluginName, getImportString(pluginName));
        }
    }

    /**
     * Stop recording.
     *
     * @return the HTML that needs to be placed in the page head in order to pull the skin extensions (JavaScript, CSS)
     *         that were required while rendering the content, since the last call to {@link #start()}
     */
    public String stop()
    {
        StringBuilder requiredSkinExtensions = new StringBuilder();
        for (Map.Entry<String, String> entry : this.requiredSkinExtensionsMap.entrySet()) {
            requiredSkinExtensions
                .append(StringUtils.removeStart(getImportString(entry.getKey()), entry.getValue()).trim());
        }
        return requiredSkinExtensions.toString();
    }

    @SuppressWarnings("deprecation")
    private String getImportString(String skinExtensionPluginName)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        Api pluginApi = xcontext.getWiki().getPluginApi(skinExtensionPluginName, xcontext);
        if (pluginApi instanceof SkinExtensionPluginApi) {
            return ((SkinExtensionPluginApi) pluginApi).getImportString();
        } else {
            return "";
        }
    }
}
