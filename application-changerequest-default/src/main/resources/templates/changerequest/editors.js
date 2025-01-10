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
'use strict';
require(['jquery', 'xwiki-meta'], function ($, xm) {
  let newGetUrl = function (overriddenMethod, action, queryString, fragment) {
    let urlParams = new URLSearchParams(window.location.search);
    let editExistingCR = urlParams.has('changerequest');
    if (action === 'edit') {
      action = 'editcr';
    }
    if (editExistingCR) {
      queryString = (queryString || '') + "&changerequest=" + urlParams.get('changerequest');
    }
    return overriddenMethod(action, queryString, fragment);
  };

  let init = function () {
    let oldGetUrl = XWiki.currentDocument.getURL.bind(XWiki.currentDocument);
    if (!XWiki.currentDocument.isGetURLOverridden) {
      XWiki.currentDocument.getURL = function (action, queryString, fragment) {
        return newGetUrl(oldGetUrl, action, queryString, fragment);
      }
      XWiki.currentDocument.isGetURLOverridden = true;
    }
    $('#tmEditObject').attr('href', XWiki.currentDocument.getURL('editcr', 'editor=object', ''));
    $('#tmEditClass').hide();
    $('#tmEditInline').hide();
    xm.refreshVersion = function () {
      console.warn("Refresh version is overridden in this editor.");
    }
  };
  $(init);
});